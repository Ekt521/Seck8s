package com.seck8s.payment.feign;

import com.seck8s.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * 调用订单服务：支付前校验订单状态与金额
 */
@FeignClient(name = "order-service", contextId = "paymentOrderClient")
public interface OrderClient {

    /** 查询订单基础信息（orderId/金额/状态），支付前校验 */
    @GetMapping("/internal/orders/{orderId}/pay-info")
    Result<PayOrderInfo> getPayInfo(@PathVariable("orderId") String orderId);

    record PayOrderInfo(String orderId, Long userId, BigDecimal amount, String orderStatus) {
    }
}
