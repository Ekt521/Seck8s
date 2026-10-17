package com.seck8s.payment.spi;

import com.seck8s.payment.entity.PaymentRecord;

/**
 * 支付网关 SPI（可插拔）：
 * 当前用 SimulatePaymentGateway 模拟，预留微信/支付宝/银联适配位。
 * 实现类注册为 Spring Bean，按 channel 分发。
 */
public interface PaymentGateway {

    /** 支持的渠道标识，如 SIMULATE / WX / ALIPAY */
    String channel();

    /**
     * 发起支付：同步返回支付单（异步出结果）。
     * 真实网关此处返回拉起支付所需参数（如二维码/跳转 URL）。
     */
    PayRequestResult createPayment(PaymentRecord record);

    /** 查询支付结果（模拟实现直接返回成功/失败） */
    PayQueryResult queryPayment(PaymentRecord record);

    /** 回调验签/校验：真实网关必须校验签名与金额 */
    boolean verifyCallback(String orderId, String sign);

    record PayRequestResult(boolean success, String payUrl) {
    }

    record PayQueryResult(boolean success, String tradeStatus) {
    }
}
