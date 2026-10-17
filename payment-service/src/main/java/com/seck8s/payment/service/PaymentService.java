package com.seck8s.payment.service;

import com.seck8s.payment.entity.PaymentRecord;

public interface PaymentService {

    /**
     * 发起支付：校验订单（UNPAID + 金额一致）→ 建支付流水（uk_order 幂等）→ 模拟渠道出账
     * @return 支付流水
     */
    PaymentRecord createPayment(Long userId, String orderId);

    /** 模拟渠道出账（异步线程）→ 写发件箱 PAY_RESULT → 发 MQ */
    void processAndNotify(PaymentRecord record);

    PaymentRecord getByPaymentId(String paymentId);
}
