package com.seck8s.common.constant;

/**
 * RabbitMQ 拓扑常量（交换机/队列/路由键），多服务消费方共用，防止字符串漂移
 */
public final class MqConstants {

    // 交换机
    public static final String ORDER_EXCHANGE = "seck8s.order.exchange"; // 订单事件 + 兼作死信交换机
    public static final String PAY_EXCHANGE = "seck8s.pay.exchange";     // 支付事件

    // 路由键
    public static final String ROUTING_ORDER_CREATED = "order.created";
    public static final String ROUTING_ORDER_CANCELLED = "order.cancelled";
    public static final String ROUTING_ORDER_PAID = "order.paid";
    public static final String ROUTING_ORDER_EXPIRED = "order.expired";
    public static final String ROUTING_PAY_RESULT = "pay.result";

    // 队列
    // 注意：RabbitMQ 同一队列多消费者是"竞争消费"（每条消息只给一个消费者）。
    // 需要"一个事件多个服务都消费"的场景（pub/sub）必须【每个消费者独立队列】+ 绑定同一路由键。
    public static final String QUEUE_ORDER_CREATE = "seck8s.order.create.queue";             // order-service 幂等建单
    public static final String QUEUE_STOCK_DEDUCT = "seck8s.stock.deduct.queue";             // inventory-service DB乐观锁扣减
    public static final String QUEUE_STOCK_RESTORE = "seck8s.stock.restore.queue";           // inventory-service 取消回补
    public static final String QUEUE_ORDER_CANCEL_NOTIFY = "seck8s.order.cancel.notify.queue"; // notification 取消通知
    public static final String QUEUE_ORDER_PAID = "seck8s.order.paid.queue";                 // notification 支付通知
    public static final String QUEUE_PAY_RESULT_ORDER = "seck8s.pay.result.order.queue";     // order-service 订单状态更新
    public static final String QUEUE_PAY_RESULT_NOTIFY = "seck8s.pay.result.notify.queue";   // notification 支付结果通知
    public static final String QUEUE_ORDER_DELAY = "seck8s.order.delay.queue";               // TTL 延迟队列（无消费者）
    public static final String QUEUE_ORDER_EXPIRED = "seck8s.order.expired.queue";           // 超时取消

    // 本地消息表业务类型
    public static final String BIZ_ORDER_CREATE = "ORDER_CREATE";
    public static final String BIZ_ORDER_CANCEL = "ORDER_CANCEL";
    public static final String BIZ_ORDER_PAID = "ORDER_PAID";
    public static final String BIZ_PAY_RESULT = "PAY_RESULT";

    private MqConstants() {
    }
}
