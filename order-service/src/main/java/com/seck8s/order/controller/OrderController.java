package com.seck8s.order.controller;

import com.seck8s.common.context.UserContext;
import com.seck8s.common.result.Result;
import com.seck8s.order.entity.Order;
import com.seck8s.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public Result<Order> getOrder(@PathVariable String orderId) {
        return Result.ok(orderService.getOrderByOrderId(orderId));
    }

    @GetMapping
    public Result<List<Order>> listOrders(@RequestParam(required = false) String status) {
        Long userId = UserContext.requireUserId();
        return Result.ok(orderService.listUserOrders(userId, status));
    }

    @DeleteMapping("/{orderId}")
    public Result<Void> cancelOrder(@PathVariable String orderId) {
        Long userId = UserContext.requireUserId();
        boolean changed = orderService.cancelOrder(orderId, userId);
        return changed ? Result.ok() : Result.error(400, "订单不存在或当前状态不可取消");
    }
}
