package com.seck8s.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seck8s.activity.dto.PreheatRequest;
import com.seck8s.activity.dto.PreheatSku;
import com.seck8s.activity.entity.Activity;
import com.seck8s.activity.entity.ActivitySku;
import com.seck8s.activity.feign.InventoryClient;
import com.seck8s.activity.mapper.ActivityMapper;
import com.seck8s.activity.mapper.ActivitySkuMapper;
import com.seck8s.activity.service.ActivityService;
import com.seck8s.common.constant.RedisKeys;
import com.seck8s.common.exception.BizException;
import com.seck8s.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private final ActivitySkuMapper activitySkuMapper;
    private final InventoryClient inventoryClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActivityServiceImpl(ActivitySkuMapper activitySkuMapper,
                               InventoryClient inventoryClient,
                               StringRedisTemplate redisTemplate) {
        this.activitySkuMapper = activitySkuMapper;
        this.inventoryClient = inventoryClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public Activity createActivity(Activity activity) {
        if (!StringUtils.hasText(activity.getActivityId())) {
            throw new BizException(ResultCode.BAD_REQUEST, "activityId 不能为空");
        }
        activity.setActivityStatus(0); // 草稿
        this.save(activity);
        return activity;
    }

    @Override
    @Transactional
    public void publish(String activityId) {
        Activity activity = getActivity(activityId);
        if (activity.getActivityStatus() != 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "活动当前状态不可发布");
        }
        List<ActivitySku> skus = listActivitySkus(activityId);
        if (skus.isEmpty()) {
            throw new BizException(ResultCode.BIZ_ERROR, "活动未挂载任何 SKU，无法发布");
        }

        // 触发库存预热
        PreheatRequest req = new PreheatRequest();
        req.setActivityId(activityId);
        List<PreheatSku> ps = skus.stream().map(as -> {
            PreheatSku p = new PreheatSku();
            p.setSkuId(as.getSkuId());
            p.setSeckillPrice(as.getSeckillPrice());
            p.setStockLimit(as.getStockLimit());
            p.setQuotaPerUser(as.getQuotaPerUser());
            return p;
        }).toList();
        req.setSkus(ps);
        try {
            com.seck8s.common.result.Result<Void> ok = inventoryClient.preheat(req);
            if (ok == null || ok.getCode() != 200) {
                throw new BizException(ResultCode.BIZ_ERROR, "库存预热失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("库存预热调用失败", e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "库存预热调用失败: " + e.getMessage());
        }

        // 更新活动状态为已发布
        activity.setActivityStatus(1);
        this.updateById(activity);

        // 写活动缓存（秒杀时 order-service 校验活动开启）
        String actKey = RedisKeys.activityKey(activityId);
        redisTemplate.opsForHash().putAll(actKey, java.util.Map.of(
                "activityId", activityId,
                "name", activity.getName(),
                "status", String.valueOf(activity.getActivityStatus()),
                "startTime", String.valueOf(activity.getStartTime()),
                "endTime", String.valueOf(activity.getEndTime())
        ));
        redisTemplate.expire(actKey, 30, TimeUnit.DAYS);

        // 写 SKU 缓存（秒杀价/限购）
        for (ActivitySku as : skus) {
            String skuKey = RedisKeys.skuKey(as.getSkuId());
            redisTemplate.opsForHash().putAll(skuKey, java.util.Map.of(
                    "skuId", as.getSkuId(),
                    "seckillPrice", as.getSeckillPrice().toPlainString(),
                    "quotaPerUser", String.valueOf(as.getQuotaPerUser())
            ));
            redisTemplate.expire(skuKey, 30, TimeUnit.DAYS);
        }

    }

    @Override
    @Transactional
    public void end(String activityId) {
        Activity activity = getActivity(activityId);
        if (activity.getActivityStatus() == 3) {
            return;
        }
        activity.setActivityStatus(3);
        this.updateById(activity);
        String actKey = RedisKeys.activityKey(activityId);
        redisTemplate.opsForHash().put(actKey, "status", "3");
    }

    @Override
    public List<Activity> listOngoing() {
        return this.list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getActivityStatus, 1)
                .orderByAsc(Activity::getStartTime));
    }

    @Override
    public Activity getActivity(String activityId) {
        Activity activity = this.getOne(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getActivityId, activityId));
        if (activity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
        }
        return activity;
    }

    //绑定商品列表到活动
    @Override
    public List<ActivitySku> listActivitySkus(String activityId) {
        return activitySkuMapper.selectList(new LambdaQueryWrapper<ActivitySku>()
                .eq(ActivitySku::getActivityId, activityId));
    }
}
