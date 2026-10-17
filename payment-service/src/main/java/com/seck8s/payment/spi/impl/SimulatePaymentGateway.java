package com.seck8s.payment.spi.impl;

import com.seck8s.payment.entity.PaymentRecord;
import com.seck8s.payment.spi.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟支付网关：可插拔实现，延迟几秒后按成功率随机出"成功/失败"结果。
 * 真实接入微信/支付宝时新增实现类替换即可。
 */
@Slf4j
@Component
public class SimulatePaymentGateway implements PaymentGateway {

    @Value("${seck8s.pay.simulate-success-rate:100}")
    private int successRate;

    @Value("${seck8s.pay.simulate-delay-seconds:3}")
    private int delaySeconds;

    @Override
    public String channel() {
        return "SIMULATE";
    }

    @Override
    public PayRequestResult createPayment(PaymentRecord record) {
        log.info("[模拟支付] 发起支付 orderId={} amount={} channel=SIMULATE", record.getOrderId(), record.getAmount());
        // 模拟支付链接（真实网关返回二维码/跳转URL）
        String payUrl = "http://localhost:8086/pay/simulate/" + record.getPaymentId();
        return new PayRequestResult(true, payUrl);
    }

    @Override
    public PayQueryResult queryPayment(PaymentRecord record) {
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean success = ThreadLocalRandom.current().nextInt(100) < successRate;
        log.info("[模拟支付] 支付结果 orderId={} result={}", record.getOrderId(), success ? "SUCCESS" : "FAILED");
        return new PayQueryResult(success, success ? "SUCCESS" : "FAILED");
    }

    @Override
    public boolean verifyCallback(String orderId, String sign) {
        return true; // 模拟：不做签名校验
    }
}
