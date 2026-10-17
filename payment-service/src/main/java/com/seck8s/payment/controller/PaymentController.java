package com.seck8s.payment.controller;

import com.seck8s.common.context.UserContext;
import com.seck8s.common.result.Result;
import com.seck8s.payment.entity.PaymentRecord;
import com.seck8s.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 发起支付 */
    @PostMapping("/{orderId}")
    public Result<PaymentRecord> pay(@PathVariable String orderId) {
        Long userId = UserContext.requireUserId();
        return Result.ok(paymentService.createPayment(userId, orderId));
    }

    /** 查询支付流水 */
    @GetMapping("/{paymentId}")
    public Result<PaymentRecord> get(@PathVariable String paymentId) {
        return Result.ok(paymentService.getByPaymentId(paymentId));
    }
}
