package com.seck8s.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seck8s.common.constant.RedisKeys;
import com.seck8s.inventory.dto.PreheatRequest;
import com.seck8s.inventory.dto.PreheatSku;
import com.seck8s.inventory.entity.SkuStock;
import com.seck8s.inventory.entity.StockFlowLog;
import com.seck8s.inventory.mapper.SkuStockMapper;
import com.seck8s.inventory.mapper.StockFlowLogMapper;
import com.seck8s.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 库存服务实现
 * <p>第一道闸门：Redis Lua 原子扣减（秒杀热路径，校验限购+扣库存一次完成）
 * <p>第二道闸门：DB 乐观锁扣减（MQ 异步落库时执行，保证 DB 与 Redis 最终一致）
 */
@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final String DEDUCT_OK = "1";
    private static final String DEDUCT_NOT_READY = "-1";
    private static final String DEDUCT_SOLD_OUT = "-2";
    private static final String DEDUCT_LIMIT = "-3";

    private final StringRedisTemplate redisTemplate;
    private final SkuStockMapper skuStockMapper;
    private final StockFlowLogMapper stockFlowLogMapper;
    private final DefaultRedisScript<Long> deductScript;

    public InventoryServiceImpl(StringRedisTemplate redisTemplate,
                                SkuStockMapper skuStockMapper,
                                StockFlowLogMapper stockFlowLogMapper) {
        this.redisTemplate = redisTemplate;
        this.skuStockMapper = skuStockMapper;
        this.stockFlowLogMapper = stockFlowLogMapper;

        this.deductScript = new DefaultRedisScript<>();
        this.deductScript.setLocation(new ClassPathResource("lua/seckill_deduct.lua"));
        this.deductScript.setResultType(Long.class);
    }

    @Override
    public int deduct(String activityId, String skuId, Long userId, Integer quantity) {
        // 从 SKU 缓存读限购数；活动剩余时长作为 bought key 的 TTL
        Map<Object, Object> skuCache = redisTemplate.opsForHash().entries(RedisKeys.skuKey(skuId));
        int quotaPerUser = skuCache.containsKey("quotaPerUser")
                ? Integer.parseInt(String.valueOf(skuCache.get("quotaPerUser")))
                : 1;
        long expireSeconds = calcActivityRemainSeconds(activityId);

        Long result = redisTemplate.execute(deductScript,
                List.of(RedisKeys.stockKey(skuId), RedisKeys.boughtKey(activityId, userId)),
                String.valueOf(quantity), String.valueOf(quotaPerUser), String.valueOf(expireSeconds));

        long code = result == null ? -99 : result;
        if (code == 1) {
            log.info("Lua扣减成功 activityId={} skuId={} userId={} qty={}", activityId, skuId, userId, quantity);
            return 1;
        }
        log.warn("Lua扣减拒绝 code={} activityId={} skuId={} userId={}", code, activityId, skuId, userId);
        return (int) code;
    }

    @Override
    @Transactional
    public boolean dbDeduct(String orderId, String skuId, Integer quantity) {
        // 幂等：该订单若已扣减过流水则跳过
        Long existing = stockFlowLogMapper.selectCount(new LambdaQueryWrapper<StockFlowLog>()
                .eq(StockFlowLog::getOrderId, orderId)
                .eq(StockFlowLog::getChangeType, "DEDUCT"));
        if (existing != null && existing > 0) {
            return true;
        }
        SkuStock stock = skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>()
                .eq(SkuStock::getSkuId, skuId));
        if (stock == null) {
            log.error("DB库存不存在 skuId={}", skuId);
            return false;
        }
        int before = stock.getStock();
        // 乐观锁：仅当库存足够才扣减，version 自增
        int updated = skuStockMapper.update(null, new LambdaUpdateWrapper<SkuStock>()
                .eq(SkuStock::getSkuId, skuId)
                .eq(SkuStock::getStock, before)
                .ge(SkuStock::getStock, quantity)
                .set(SkuStock::getStock, before - quantity)
                .set(SkuStock::getVersion, stock.getVersion() + 1));
        if (updated == 0) {
            log.error("DB乐观锁扣减失败 skuId={} before={} qty={}", skuId, before, quantity);
            return false;
        }
        writeFlow(skuId, orderId, "DEDUCT", -quantity, before, before - quantity);
        return true;
    }

    @Override
    @Transactional
    public void restore(String orderId, String skuId, Integer quantity) {
        // 幂等：该订单已回补过则跳过
        Long existing = stockFlowLogMapper.selectCount(new LambdaQueryWrapper<StockFlowLog>()
                .eq(StockFlowLog::getOrderId, orderId)
                .eq(StockFlowLog::getChangeType, "RESTORE"));
        if (existing != null && existing > 0) {
            return;
        }
        // 1) Redis 回补
        Long newStock = redisTemplate.opsForValue().increment(RedisKeys.stockKey(skuId), quantity);
        // 2) DB 乐观锁恢复
        SkuStock stock = skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>()
                .eq(SkuStock::getSkuId, skuId));
        if (stock != null) {
            int before = stock.getStock();
            skuStockMapper.update(null, new LambdaUpdateWrapper<SkuStock>()
                    .eq(SkuStock::getSkuId, skuId)
                    .set(SkuStock::getStock, before + quantity)
                    .set(SkuStock::getVersion, stock.getVersion() + 1));
            writeFlow(skuId, orderId, "RESTORE", quantity, before, before + quantity);
        }
        log.info("库存回补 orderId={} skuId={} qty={} redisNew={}", orderId, skuId, quantity, newStock);
    }

    @Override
    @Transactional
    public void preheat(PreheatRequest request) {
        for (PreheatSku ps : request.getSkus()) {
            // 1) Redis 库存 key：SET NX 保证只预热一次
            String stockKey = RedisKeys.stockKey(ps.getSkuId());
            Boolean set = redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(ps.getStockLimit()));
            log.info("预热 skuId={} stockLimit={} setIfAbsent={}", ps.getSkuId(), ps.getStockLimit(), set);

            // 2) SKU 缓存（秒杀价/限购）
            redisTemplate.opsForHash().putAll(RedisKeys.skuKey(ps.getSkuId()), Map.of(
                    "skuId", ps.getSkuId(),
                    "seckillPrice", ps.getSeckillPrice().toPlainString(),
                    "quotaPerUser", String.valueOf(ps.getQuotaPerUser())
            ));

            // 3) DB 初始化（幂等 upsert：已有则不动）
            SkuStock stock = skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>()
                    .eq(SkuStock::getSkuId, ps.getSkuId()));
            if (stock == null) {
                stock = new SkuStock();
                stock.setSkuId(ps.getSkuId());
                stock.setStock(ps.getStockLimit());
                stock.setVersion(0);
                skuStockMapper.insert(stock);
                writeFlow(ps.getSkuId(), null, "INIT", ps.getStockLimit(), 0, ps.getStockLimit());
            }
        }
    }

    @Override
    public Integer getStock(String skuId) {
        try {
            String stock = redisTemplate.opsForValue().get(RedisKeys.stockKey(skuId));
            if (stock != null) {
                return Integer.valueOf(stock);
            }
        } catch (Exception e) {
            log.warn("Redis inventory lookup failed, falling back to DB skuId={} err={}", skuId, e.getMessage());
        }
        // 回源 DB
        SkuStock skuStock = skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>()
                .eq(SkuStock::getSkuId, skuId));
        if (skuStock == null || skuStock.getStock() == null) {
            return null;
        }
        // Redis key 丢失时用数据库库存恢复缓存，保证查询与秒杀读取同一库存源。
        try {
            redisTemplate.opsForValue().setIfAbsent(
                    RedisKeys.stockKey(skuId), String.valueOf(skuStock.getStock()));
        } catch (Exception e) {
            log.warn("库存缓存回填失败 skuId={} err={}", skuId, e.getMessage());
        }
        return skuStock.getStock();
    }

    /** 计算活动剩余秒数（从活动缓存 endTime；无缓存默认 1 小时） */
    private long calcActivityRemainSeconds(String activityId) {
        try {
            Object end = redisTemplate.opsForHash().get(RedisKeys.activityKey(activityId), "endTime");
            if (end != null) {
                String endText = String.valueOf(end);
                java.time.LocalDateTime endTime;
                try {
                    endTime = java.time.LocalDateTime.parse(endText,
                            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (java.time.format.DateTimeParseException ignored) {
                    endTime = java.time.LocalDateTime.parse(endText,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                }
                long remain = java.time.Duration.between(java.time.LocalDateTime.now(), endTime).getSeconds();
                if (remain > 0) {
                    return remain;
                }
            }
        } catch (Exception e) {
            log.warn("计算活动剩余时间失败 activityId={}: {}", activityId, e.getMessage());
        }
        return 3600L;
    }

    private void writeFlow(String skuId, String orderId, String changeType, int qty, int before, int after) {
        StockFlowLog flow = new StockFlowLog();
        flow.setSkuId(skuId);
        flow.setOrderId(orderId);
        flow.setChangeType(changeType);
        flow.setChangeQty(qty);
        flow.setBeforeStock(before);
        flow.setAfterStock(after);
        stockFlowLogMapper.insert(flow);
    }
}
