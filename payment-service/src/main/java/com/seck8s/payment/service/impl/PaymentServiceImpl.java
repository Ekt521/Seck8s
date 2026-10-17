package com.seck8s.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.event.PayResultEvent;
import com.seck8s.common.exception.BizException;
import com.seck8s.common.result.Result;
import com.seck8s.common.result.ResultCode;
import com.seck8s.common.util.OrderNoGenerator;
import com.seck8s.payment.entity.MessageOutbox;
import com.seck8s.payment.entity.PaymentRecord;
import com.seck8s.payment.feign.OrderClient;
import com.seck8s.payment.mapper.PaymentRecordMapper;
import com.seck8s.payment.service.PaymentOutboxService;
import com.seck8s.payment.service.PaymentService;
import com.seck8s.payment.spi.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付服务核心：校验订单 → 建支付流水（uk_order 幂等）→ 模拟渠道异步出账 → 发件箱 PAY_RESULT
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final PaymentOutboxService outboxService;
    private final OrderNoGenerator orderNoGenerator;
    private final OrderClient orderClient;
    private final Map<String, PaymentGateway> gatewayMap;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentServiceImpl(PaymentRecordMapper paymentRecordMapper,
                              PaymentOutboxService outboxService,
                              OrderNoGenerator orderNoGenerator,
                              OrderClient orderClient,
                              List<PaymentGateway> gateways) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.outboxService = outboxService;
        this.orderNoGenerator = orderNoGenerator;
        this.orderClient = orderClient;
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::channel, Function.identity()));
    }

    @Override
    @Transactional
    public PaymentRecord createPayment(Long userId, String orderId) {
        // 1. 校验订单：存在且 UNPAID
        Result<OrderClient.PayOrderInfo> payInfoResult = orderClient.getPayInfo(orderId);
        OrderClient.PayOrderInfo payInfo = payInfoResult.getData();
        if (payInfo == null || !"UNPAID".equals(payInfo.orderStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "订单不存在或不可支付");
        }
        if (!payInfo.userId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权支付该订单");
        }

        // 2. 幂等：同一订单只建一条支付流水（uk_order）
        PaymentRecord existing = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId));
        if (existing != null) {
            return existing;
        }

        // 3. 建支付流水
        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(orderNoGenerator.nextOrderId());
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setAmount(payInfo.amount());
        record.setChannel("SIMULATE");
        record.setPayStatus("CREATED");
        paymentRecordMapper.insert(record);

        // 4. 异步模拟出账（不阻塞用户请求），出账结果通过发件箱发 PAY_RESULT
        PaymentRecord snapshot = record;
        CompletableFuture.runAsync(() -> processAndNotify(snapshot));
        return record;
    }

    @Override
    @Transactional
    public void processAndNotify(PaymentRecord record) {
        try {
            PaymentGateway gateway = gatewayMap.getOrDefault(record.getChannel(),
                    gatewayMap.values().iterator().next());
            // 调用模拟渠道查询结果（内部延迟 + 按成功率）
            PaymentGateway.PayQueryResult query = gateway.queryPayment(record);
            String status = query.success() ? "SUCCESS" : "FAILED";

            // 更新支付流水状态
            PaymentRecord update = new PaymentRecord();
            update.setId(record.getId());
            update.setPayStatus(status);
            paymentRecordMapper.updateById(update);

            // 事务内写发件箱 PAY_RESULT
            PayResultEvent event = new PayResultEvent();
            event.setPaymentId(record.getPaymentId());
            event.setOrderId(record.getOrderId());
            event.setUserId(record.getUserId());
            event.setAmount(record.getAmount());
            event.setResult(status);

            MessageOutbox msg = new MessageOutbox();
            msg.setMsgId(UUID.randomUUID().toString());
            msg.setBizType(MqConstants.BIZ_PAY_RESULT);
            msg.setBizKey(record.getOrderId());
            msg.setExchange(MqConstants.PAY_EXCHANGE);
            msg.setRoutingKey(MqConstants.ROUTING_PAY_RESULT);
            msg.setPayload(objectMapper.writeValueAsString(event));
            outboxService.save(msg);

            log.info("支付完成 orderId={} paymentId={} result={}", record.getOrderId(), record.getPaymentId(), status);
        } catch (Exception e) {
            log.error("支付出账处理异常 paymentId={}", record.getPaymentId(), e);
            // 标记失败并投递失败结果，由 order 侧超时取消兜底
        }
    }

    @Override
    public PaymentRecord getByPaymentId(String paymentId) {
        return paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPaymentId, paymentId));
    }
}
