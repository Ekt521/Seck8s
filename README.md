# Seck8s — 票务秒杀微服务系统

基于 Spring Cloud 的**在线票务秒杀系统**，核心目标：承载瞬时高并发抢票流量，在「查票 → 秒杀 → 支付」全链路上保证**数据一致性、杜绝超卖**。

采用 **Redis Lua 原子扣减 + DB 乐观锁 + 状态受限 UPDATE** 三重防线防超卖，**RabbitMQ + 本地消息表（发件箱）** 保证异步事件可靠投递，**DLX 延迟队列 + 定时扫描** 双通道兜底超时取消与库存回补。

---

## 整体架构

```
浏览器 / App
     │
     ▼
Spring Cloud Gateway (seck8s-gateway :8080)   JWT 校验 | 路由 | 秒杀接口限流 | X-User-Id 透传
     │
     ├────► user-service        :8081  user_db       注册/登录(BCrypt+JWT)
     ├────► ticket-service      :8082  ticket_db      查票/查SKU(门票 + 场次库存展示)
     ├────► activity-service    :8083  activity_db    活动生命周期；发布时触发库存预热
     ├────► order-service       :8085  order_db       秒杀编排 | 本地消息表 | 异步建单 | 超时取消 | 支付回调
     ├────► payment-service     :8086  payment_db     模拟支付网关(可插拔) | 本地消息表
     └────► notification-service :8087 notification_db 消费订单/支付事件，幂等写站内通知

inventory-service :8084  inventory_db   库存唯一真相：Redis Lua 原子扣减(第一道) + DB 乐观锁(第二道)

服务间协作：order→inventory 走 OpenFeign(秒杀热路径)，其余跨服务联动全部走 RabbitMQ 事件
基础设施：Nacos 注册中心 :8848 | MySQL :3306(7 库) | Redis :6379 | RabbitMQ :5672/15672
```

| 端口   | 服务                     |
| ---- | ---------------------- |
| 8080 | seck8s-gateway（唯一对外入口） |
| 8081 | user-service           |
| 8082 | ticket-service         |
| 8083 | activity-service       |
| 8084 | inventory-service      |
| 8085 | order-service          |
| 8086 | payment-service        |
| 8087 | notification-service   |

---

## 技术栈

- Java 17 · Spring Boot 3.4.3 · Spring Cloud 2024.0.0 (Moorgate) · Spring Cloud Alibaba 2023.0.3.2
- 注册中心：Nacos 2.4.2（standalone，:8848）
- 数据库：MySQL 8（database-per-service，7 个 schema）
- 缓存：Redis（Lua 脚本原子扣减）
- 消息：RabbitMQ（topic 交换机 pub/sub，DLX + per-message TTL 延迟队列）
- 认证：JWT（jjwt 0.12.6，HS384），网关统一鉴权，Feign 头透传
- 构建：Maven 多模块，spring-boot-maven-plugin 打成可执行 fat jar

---

## 目录结构

```
Seck8s/
├── pom.xml                      父 POM（SCA BOM、公共依赖、-parameters、打包插件）
├── seck8s-common/               公共模块：Result/BizException/JwtUtil/常量/Snowflake/Feign拦截器
│   └── src/main/resources/common-infra.yml   基础设施共享配置（改一处全生效）
├── seck8s-gateway/              网关（WebFlux）：路由/JWT/限流
├── user-service/ · ticket-service/ · activity-service/
├── inventory-service/           库存服务（防超卖核心）
├── order-service/               订单服务（秒杀编排核心）
├── payment-service/ · notification-service/
└── deploy/
    ├── mysql/init/              7 库 DDL + 演示数据（手动执行）
    ├── start-all.cmd            一键启动 8 服务
    └── seckill-test.py          并发压测脚本（防超卖验证）
```

---

## 快速开始

### 1. 初始化数据库

在 MySQL（127.0.0.1:3306，root）中按顺序执行 `deploy/mysql/init/` 下的脚本：

```
mysql -uroot -p < deploy/mysql/init/01-create-databases.sql
mysql -uroot -p < deploy/mysql/init/10-user_db.sql
mysql -uroot -p < deploy/mysql/init/20-ticket_db.sql
mysql -uroot -p < deploy/mysql/init/30-activity_db.sql
mysql -uroot -p < deploy/mysql/init/40-inventory_db.sql
mysql -uroot -p < deploy/mysql/init/50-order_db.sql
mysql -uroot -p < deploy/mysql/init/60-payment_db.sql
mysql -uroot -p < deploy/mysql/init/70-notification_db.sql
mysql -uroot -p < deploy/mysql/init/99-seed-demo-data.sql
```

数据库连接、Redis 地址、RabbitMQ、Nacos 等基础设施配置集中在 `seck8s-common/src/main/resources/common-infra.yml`（7 个服务通过 `spring.config.import` 共用）。**密码和 JWT 密钥不写进仓库**，运行时通过环境变量注入：

| 环境变量                     | 用途                                      |
| ------------------------ | --------------------------------------- |
| `SECK8S_MYSQL_PASSWORD`  | MySQL root 密码                           |
| `SECK8S_REDIS_PASSWORD`  | Redis 密码                                |
| `SECK8S_RABBITMQ_PASSWORD` | RabbitMQ 密码                             |
| `SECK8S_JWT_SECRET`      | JWT HS256 签名密钥（长度需 ≥ 32 字节）              |

Windows 一键启动：把 `deploy/local.env.example.cmd` 复制为 `deploy/local.env.cmd`，填入真实值；`start-all.cmd` 会自动加载它。`local.env.cmd` 已被 `.gitignore` 忽略，不会提交。

用 IDEA 单独启动某个服务时，请在该服务的 Run Configuration → Environment variables 中配置这四个变量。

> ⚠️ 注意：`common-infra.yml` 中**不要**为 `infra.mysql.schema` 设置默认值——`spring.config.import` 的优先级高于各服务自己的 application.yml，默认值会覆盖每个服务各自的库名。

### 2. 启动基础设施

| 组件       | 启动方式                                                                 |
| -------- | -------------------------------------------------------------------- |
| MySQL    | 本机服务（已运行）                                                            |
| Redis    | 远端已运行（10.163.55.111:6379，密码由 SECK8S_REDIS_PASSWORD 提供）                       |
| RabbitMQ | Docker 容器（用户 guest，密码由 SECK8S_RABBITMQ_PASSWORD 提供，5672/15672）                                    |
| Nacos    | `D:\DevTools\nacos-2.4.2\nacos\bin\startup.cmd -m standalone`（:8848） |

### 3. 构建

```
mvn clean package -DskipTests
```

### 4. 启动 8 个服务

```
deploy\start-all.cmd
```

等待 30–60s 注册完成。全部健康检查：`http://127.0.0.1:8080/actuator/health`（网关 404 属正常）。

---

## 接口清单（统一经网关 :8080）

| 方法   | 路径                                   | 说明                | 认证     |
| ---- | ------------------------------------ | ----------------- | ------ |
| POST | /api/users/register                  | 注册（BCrypt 加密）     | 免      |
| POST | /api/users/login                     | 登录，返回 JWT         | 免      |
| GET  | /api/users/profile                   | 获取个人信息            | Bearer |
| GET  | /api/tickets/list                    | 查票列表              | 免      |
| GET  | /api/tickets/activities/{activityId} | 查活动 + SKU（含实时库存）  | 免      |
| POST | /api/activities/publish              | 发布活动（触发库存预热，幂等）   | 免      |
| POST | /api/seckill                         | **秒杀抢购**（核心热路径）   | Bearer |
| GET  | /api/orders/{orderId}                | 查询订单状态（前端轮询）      | Bearer |
| GET  | /api/orders/my                       | 我的订单              | Bearer |
| POST | /api/payments/{orderId}              | 发起支付（模拟渠道，延迟几秒回调） | Bearer |
| GET  | /api/payments/{orderId}/status       | 查询支付状态            | Bearer |

内部接口（不经网关，服务间 Feign 调用）：`inventory-service :8084/internal/inventory/{deduct,restore,preheat,stock/{skuId}}`。

**秒杀请求体**：

```json
POST /api/seckill
Authorization: Bearer <JWT>
{"activityId":"ACT20260804001","skuId":"SKU002","quantity":1}

→ 200 {"code":200,"data":{"status":"CREATING","orderId":"...","message":"订单处理中"}}
   status ∈ CREATING | FAIL_SOLD_OUT(已售罄) | FAIL_LIMIT(超出限购) | FAIL_CLOSED(未开始)
```

秒杀成功立即返回 `orderId`，前端轮询 `GET /api/orders/{orderId}`（结果缓存 `seck8s:seckill:result:{orderId}`）直至 `CREATED/PAID/CANCELLED`。

---

## 核心秒杀链路（防超卖）

```
① 预热   活动发布 → activity → inventory.preheat → Redis SET NX 库存 + SKU缓存(秒杀价/限购) + DB sku_stock 初始化
② 秒杀   POST /api/seckill → order 校验活动 → Feign 调 inventory.deduct
        → Redis Lua 一次原子完成「库存够?→扣库存 + 限购计数+1」，返回 1/-1未预热/-2售罄/-3超限购
        → 成功：生成 Snowflake orderId → 事务内写发件箱(ORDER_CREATE, status=0) → 返回 {CREATING, orderId}
③ 落库   发件箱调度器(1s) 扫 status=0 → 发 MQ(order.created, confirm) → 置 status=1
        → topic 广播两个独立队列：
          · order-service  幂等建单（orders.uk_order_id 唯一键）
          · inventory-service DB 乐观锁扣减（WHERE stock>=qty + version 自增 + DEDUCT 流水）
④ 超时   DLX+TTL 延迟队列(建单即投，per-message TTL=15min) → 死信 order.expired → 幂等取消
        ＋ 定时扫描兜底(60s，UNPAID 且超支付截止时间)
        → UPDATE orders SET CANCELLED WHERE status='UNPAID'（affected=0 忽略）
        → 发 order.cancelled → inventory Redis INCRBY + DB 恢复 + RESTORE 流水
⑤ 支付   POST /api/payments/{orderId} → 建 payment_record(uk_order_id 一单一支付) → 模拟渠道延迟回调
        → 发件箱 PAY_RESULT → pay.result → order 幂等 UPDATE orders SET PAID WHERE status='UNPAID'
        → 发 order.paid → notification 幂等写通知（notify type+orderId+userId 唯一）
```

### 防超卖三重保障

1. **Redis Lua 原子扣减**：`lua/seckill_deduct.lua` 一条脚本内完成库存判断 + 扣减 + 限购计数，秒杀热路径无竞态；
2. **DB 乐观锁扣减**：异步落库时 `UPDATE sku_stock SET stock=stock-qty, version=version+1 WHERE sku_id=? AND stock>=qty`，与 Redis 最终一致；
3. **状态受限 UPDATE**：取消只允许 `UNPAID→CANCELLED`、支付只允许 `UNPAID→PAID`，保证**库存只扣一次、只回补一次**。

### 可靠性与幂等设计

| 场景   | 机制                                                 |
| ---- | -------------------------------------------------- |
| 发件箱  | 业务事务与写 outbox 同库，`status=0→1→2` + 重试，确保消息不丢不重      |
| 建单幂等 | `orders.uk_order_id` 唯一键，重复 order.created 直接忽略     |
| 扣减幂等 | `stock_flow_log` DEDUCT 流水按 order_id 去重            |
| 支付幂等 | `payment_record.uk_order_id` 一单一支付；PAID 状态受限更新     |
| 回补幂等 | RESTORE 流水按 order_id 去重 + 仅 UNPAID 可取消             |
| 通知幂等 | `notification` (type, order_id, user_id) 唯一索引      |
| 广播隔离 | 每个消费者独立队列绑定同一 routing key（topic pub/sub），避免竞争消费丢事件 |
| 延迟队列 | 队列级 TTL 15min + per-message TTL 覆盖，到期转死信；定时扫描兜底防丢  |

### Redis 键设计

| Key                                   | 类型     | 说明                              |
| ------------------------------------- | ------ | ------------------------------- |
| `seck8s:stock:{skuId}`                | string | 剩余库存（Lua 扣减）                    |
| `seck8s:bought:{activityId}:{userId}` | string | 限购计数，TTL=活动剩余时长                 |
| `seck8s:activity:{activityId}`        | hash   | 活动/场次信息缓存                       |
| `seck8s:sku:{skuId}`                  | hash   | SKU 秒杀价 + 限购数                   |
| `seck8s:seckill:result:{orderId}`     | string | CREATING/CREATED/PAID/CANCELLED |

---

## 压测方法（防超卖验证）

> `seckill-bench.py` 需要 MySQL 和 Redis 密码，运行前先 `call deploy\local.env.cmd`（或在当前终端设置对应环境变量）。

```
cd deploy
python seckill-test.py 250
```

脚本逻辑：并发注册+登录 250 个用户 → 对 SKU002（库存 200、限购 1）并发秒杀 → 等待异步落库 → 校验：

- 成功下单数 ≤ 放量（200），拒绝数 = 溢出数
- Redis 库存精确归零、**不为负**
- 订单数 = DEDUCT 流水数 = 200；同用户重复订单 = 0

### 实测结果（SKU002 放量 200 / 限购 1 / 250 并发）

```
秒杀: 200 个进入 CREATING, 50 个 FAIL_SOLD_OUT, 耗时 0.5s
Redis 库存: 200 → 0
DB 库存: SKU002 = 0（从未为负）
stock_flow_log: DEDUCT = 200 = 订单数 200, RESTORE = 0
同用户重复下单: 0
```

**无超卖 ✅**

### 幂等 & 超时验证

| 场景   | 操作                            | 结果                                                     |
| ---- | ----------------------------- | ------------------------------------------------------ |
| 建单幂等 | 向交换机重投重复 `order.created`      | 订单仍 1 条、DEDUCT 流水仍 1 条 ✅                               |
| 超时取消 | 向延迟队列投 TTL=3s 消息触发死信          | UNPAID→CANCELLED，Redis/DB 库存 0→1，RESTORE 流水 1，取消通知 1 ✅ |
| 取消幂等 | 对已 CANCELLED 订单再投过期消息         | 无二次回补，库存/RESTORE 不变 ✅                                  |
| 扫描兜底 | 回拨订单 expire_time 到过去，等 60s 扫描 | 自动 CANCELLED + 回补 ✅                                    |

---

## 故障假设（设计内边界）

- **Redis 重启丢 key**：`getStock` 回源 DB；活动重新发布（publish 幂等）可重建库存；
- **消息消费失败**：`acknowledge-mode: manual`，异常 nack 重回队列重试；超限进发件箱 `status=2` 告警；
- **延迟消息丢失**：定时扫描兜底（60s）不依赖 MQ 仍能取消超时订单；
- **MySQL 不可用期间**：秒杀热路径只依赖 Redis Lua，可继续放行，DB 落库由 MQ 消费积压、恢复后补齐（发件箱保证不丢）。

---

## 常见问题

| 症状                              | 原因 / 处理                                                                                     |
| ------------------------------- | ------------------------------------------------------------------------------------------- |
| 服务启动失败：`missing parameter name` | 需 `-parameters` 编译参数（父 POM 已配，`mvn clean` 重新打包）                                             |
| 网关全部 401                        | 白名单走 `@ConfigurationProperties(Seck8sGatewayProperties)`，改 `seck8s.gateway.whitelist` 后重启网关 |
| 某队列消费竞争丢消息                      | 一个队列多个消费者 = 竞争消费；广播事件必须**每消费者独立队列**绑定同一 routing key                                         |
| 注册/登录中文乱码 400                   | Git Bash 下 curl 中文是 GBK；压测用 ASCII 用户名，或改用 Postman                                           |
