package com.seck8s.order.controller;

import com.seck8s.common.result.Result;
import com.seck8s.order.entity.Order;
import com.seck8s.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 订单内部接口（支付服务调用，不走网关）
 */
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 支付前校验：返回订单金额/状态/用户 */
    @GetMapping("/{orderId}/pay-info")
    public Result<PayOrderInfo> getPayInfo(@PathVariable String orderId) {
        Order order = orderService.getOrderByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.ok(new PayOrderInfo(order.getOrderId(), order.getUserId(),
                order.getAmount(), order.getOrderStatus()));
    }

    public record PayOrderInfo(String orderId, Long userId, BigDecimal amount, String orderStatus) {
    }
}
