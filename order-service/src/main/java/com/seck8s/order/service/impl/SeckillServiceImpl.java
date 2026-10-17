package com.seck8s.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.constant.RedisKeys;
import com.seck8s.common.event.OrderEvent;
import com.seck8s.common.exception.BizException;
import com.seck8s.common.result.ResultCode;
import com.seck8s.common.util.OrderNoGenerator;
import com.seck8s.order.dto.DeductRequest;
import com.seck8s.order.dto.DeductResponse;
import com.seck8s.order.dto.SeckillRequest;
import com.seck8s.order.dto.SeckillResult;
import com.seck8s.order.entity.MessageOutbox;
import com.seck8s.order.feign.InventoryClient;
import com.seck8s.order.service.OutboxService;
import com.seck8s.order.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * 秒杀编排（核心）：
 * 校验活动开启 → Feign 调 inventory Redis Lua 原子扣减 → 事务写发件箱 order.created → 结果缓存
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    private final InventoryClient inventoryClient;
    private final OutboxService outboxService;
    private final OrderNoGenerator orderNoGenerator;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SeckillServiceImpl(InventoryClient inventoryClient,
                              OutboxService outboxService,
                              OrderNoGenerator orderNoGenerator,
                              StringRedisTemplate redisTemplate) {
        this.inventoryClient = inventoryClient;
        this.outboxService = outboxService;
        this.orderNoGenerator = orderNoGenerator;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SeckillResult seckill(Long userId, SeckillRequest request) {
        String activityId = request.getActivityId();
        String skuId = request.getSkuId();
        Integer qty = request.getQuantity() == null ? 1 : request.getQuantity();
        if (activityId == null || skuId == null || qty < 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "参数不合法");
        }

        // 1. 校验活动开启（活动缓存；未预热直接拒绝）
        Map<Object, Object> activity = redisTemplate.opsForHash().entries(RedisKeys.activityKey(activityId));
        if (activity.isEmpty()) {
            return SeckillResult.fail("FAIL_NOT_START", "活动未开始或未预热");
        }
        String actStatus = String.valueOf(activity.getOrDefault("status", ""));
        if ("3".equals(actStatus)) {
            return SeckillResult.fail("FAIL_NOT_START", "活动已结束");
        }
        if (!isWithinActivityWindow(activity)) {
            return SeckillResult.fail("FAIL_NOT_START", "活动未开始或已结束");
        }

        // 2. 读 SKU 缓存（秒杀价），校验参与
        Map<Object, Object> skuCache = redisTemplate.opsForHash().entries(RedisKeys.skuKey(skuId));
        if (skuCache.isEmpty()) {
            return SeckillResult.fail("FAIL_NOT_START", "SKU 未参与该活动");
        }

        // 3. Feign 调 inventory Redis Lua 原子扣减（限购+库存一次完成）
        DeductRequest deductRequest = new DeductRequest();
        deductRequest.setActivityId(activityId);
        deductRequest.setSkuId(skuId);
        deductRequest.setUserId(userId);
        deductRequest.setQuantity(qty);
        DeductResponse deductResponse;
        try {
            deductResponse = inventoryClient.deduct(deductRequest);
        } catch (Exception e) {
            log.error("库存服务调用失败", e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "库存服务暂不可用，请重试");
        }
        switch (deductResponse.getCode()) {
            case -1 -> {
                return SeckillResult.fail("FAIL_NOT_START", "活动未开始或未预热");
            }
            case -2 -> {
                return SeckillResult.fail("FAIL_SOLD_OUT", "手慢了，已被抢光");
            }
            case -3 -> {
                return SeckillResult.fail("FAIL_LIMIT", "超出限购数量");
            }
            case 1 -> {
                // 4. 扣减成功：生成 orderId，构造事件，写本地消息表（与后续订单落库解耦）
                return buildOrderEvent(userId, request, skuCache, qty);
            }
            default -> {
                return SeckillResult.fail("FAIL_SOLD_OUT", "扣减失败");
            }
        }
    }

    /** 扣减成功 → 生成订单号、构造事件、写发件箱、写结果缓存 */
    private SeckillResult buildOrderEvent(Long userId, SeckillRequest request, Map<Object, Object> skuCache, Integer qty) {
        String orderId = orderNoGenerator.nextOrderId();

        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setActivityId(request.getActivityId());
        event.setSkuId(request.getSkuId());
        event.setQuantity(qty);
        event.setEventType("ORDER_CREATE");
        // 金额 = 秒杀价 * 数量
        String seckillPrice = String.valueOf(skuCache.getOrDefault("seckillPrice", "0"));
        try {
            event.setAmount(new BigDecimal(seckillPrice).multiply(BigDecimal.valueOf(qty)));
        } catch (Exception e) {
            event.setAmount(BigDecimal.ZERO);
        }

        // 写本地消息表（同一事务内仅插入发件箱；订单行由消费端幂等落库）
        try {
            MessageOutbox msg = new MessageOutbox();
            msg.setMsgId(UUID.randomUUID().toString());
            msg.setBizType(MqConstants.BIZ_ORDER_CREATE);
            msg.setBizKey(orderId);
            msg.setExchange(MqConstants.ORDER_EXCHANGE);
            msg.setRoutingKey(MqConstants.ROUTING_ORDER_CREATED);
            msg.setPayload(objectMapper.writeValueAsString(event));
            outboxService.save(msg);
        } catch (Exception e) {
            log.error("写发件箱失败", e);
            // Redis 扣减与订单发件箱不是同一事务，落库失败时必须回补库存。
            try {
                inventoryClient.restore(new com.seck8s.order.dto.RestoreRequest(orderId, request.getSkuId(), qty));
            } catch (Exception restoreError) {
                log.error("订单发件箱失败后库存回补失败 orderId={}", orderId, restoreError);
            }
            throw new BizException(ResultCode.SYSTEM_ERROR, "系统繁忙，请重试");
        }

        // 写秒杀结果缓存（前端轮询 GET /api/orders/{orderId}）
        redisTemplate.opsForValue().set(RedisKeys.seckillResultKey(orderId), "CREATING",
                Duration.ofHours(2));

        return SeckillResult.creating(orderId);
    }

    private boolean isWithinActivityWindow(Map<Object, Object> activity) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = parseActivityTime(activity.get("startTime"));
            LocalDateTime end = parseActivityTime(activity.get("endTime"));
            return (start == null || !now.isBefore(start)) && (end == null || now.isBefore(end));
        } catch (Exception e) {
            log.warn("活动时间解析失败，拒绝秒杀 activity={}", activity, e);
            return false;
        }
    }

    private LocalDateTime parseActivityTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        String text = String.valueOf(value);
        try {
            return LocalDateTime.parse(text, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(text, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        }
    }
}
