package com.seck8s.order.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seck8s.order.entity.Order;
import com.seck8s.order.mapper.OrderMapper;
import com.seck8s.order.service.OrderExpireService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时定时扫描（兜底）：每分钟扫 UNPAID 且已过支付截止时间的订单，幂等取消
 * 主路径是 DLX+TTL 延迟消息，此处兜底防止延迟消息丢失
 */
@Slf4j
@Component
@EnableScheduling
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;
    private final OrderExpireService orderExpireService;

    public OrderTimeoutJob(OrderMapper orderMapper, OrderExpireService orderExpireService) {
        this.orderMapper = orderMapper;
        this.orderExpireService = orderExpireService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void scanExpiredOrders() {
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderStatus, "UNPAID")
                .lt(Order::getExpireTime, LocalDateTime.now())
                .last("limit 200"));
        if (expired.isEmpty()) {
            return;
        }
        log.info("定时扫描发现 {} 笔超时订单，开始取消", expired.size());
        for (Order order : expired) {
            try {
                orderExpireService.cancelExpiredOrder(order.getOrderId());
            } catch (Exception e) {
                log.error("定时取消失败 orderId={}", order.getOrderId(), e);
            }
        }
    }
}
