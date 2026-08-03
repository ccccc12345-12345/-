# 前后端实时数据同步实施计划

## Context

当前项目是钓鱼场预约/餐厅/商城综合系统：
- 后端：Spring Boot 2.7.18 + Java 8 + MyBatis-Plus + Redis/Redisson + MySQL
- 前端：fishing-admin（商家后台）和 fishing-pc（用户端+商家端），均为 Vue3 + Vite + ElementPlus + Pinia + axios
- 认证：JWT 通过 `Authorization: Bearer <token>` 传递，`LoginInterceptor` 设置 `CurrentUser` ThreadLocal

**问题**：用户端提交餐厅订单、预约、商城订单后，商家端目前只能依赖手动刷新或 30 秒轮询查看新数据，无法做到“实时”。

**目标**：建立一套可扩展的实时推送机制，使商家端在发生业务变更时立即刷新对应页面；同时覆盖多实例部署场景。

## Scope

经确认，本次实施范围包含全商家端可见数据：
1. 餐厅订单（创建、支付、状态变更）
2. 预约（创建、取消、过期、金额变更、抽号结果变更）
3. 商城订单（创建、支付、状态变更）
4. 核销签到（预约核销）
5. 钓位看板（钓位状态变化、抽号结果变化）
6. 商家工作台统计（收入、预约数、核销数、上座率）

部署形态：支持多实例，需通过 Redis Pub/Sub 做跨实例广播。

## Recommended Approach

采用 **SSE（Server-Sent Events）+ Spring ApplicationEvent + Redis Pub/Sub** 架构：

```
用户下单/预约/核销/状态变更
        ↓
Service 层事务提交后发布 NotificationEvent（携带 merchantId、pondId、摘要）
        ↓
NotificationEventListener 监听
        ↓
RedisNotificationBridge 将事件发布到 Redis 频道 notify:merchant:<merchantId>
        ↓
各实例 RedisMessageListener 接收后调用 MerchantSseService
        ↓
SseEmitterRepository 按 merchantId 广播到所有在线 SseEmitter
        ↓
前端 EventSource 接收 → 触发对应页面 load() 刷新
```

**为什么用 SSE**：
- 业务只需要服务端向商家单向推送；
- 复用现有 HTTP/JWT 认证体系，无需额外协议；
- 比 WebSocket 轻量，兼容 Nginx/代理；
- 天然支持自动重连。

## Backend Design

### 新增文件

| 文件 | 说明 |
|------|------|
| `src/main/java/com/example/fishing/notify/NotificationEventType.java` | 事件类型枚举 |
| `src/main/java/com/example/fishing/notify/NotificationEvent.java` | Spring ApplicationEvent 子类 |
| `src/main/java/com/example/fishing/notify/NotificationPayload.java` | 通用负载封装 |
| `src/main/java/com/example/fishing/notify/SseEmitterRepository.java` | merchantId -> emitters 内存仓库 |
| `src/main/java/com/example/fishing/notify/MerchantSseService.java` | SSE 服务接口 |
| `src/main/java/com/example/fishing/notify/MerchantSseServiceImpl.java` | SSE 服务实现 |
| `src/main/java/com/example/fishing/notify/MerchantSseController.java` | `/api/merchant/sse/connect` 接口 |
| `src/main/java/com/example/fishing/notify/NotificationEventListener.java` | 本地事件监听并桥接到 Redis |
| `src/main/java/com/example/fishing/notify/RedisNotificationBridge.java` | Redis Pub/Sub 发布 |
| `src/main/java/com/example/fishing/notify/RedisNotificationListener.java` | Redis 订阅并转成本地 SSE 推送 |
| `src/main/java/com/example/fishing/config/AsyncConfig.java` | 可选：配置 `@Async` 线程池 |

### 关键类签名

```java
public enum NotificationEventType {
    RESTAURANT_ORDER_CREATED,
    RESTAURANT_ORDER_STATUS_CHANGED,
    SHOP_ORDER_CREATED,
    SHOP_ORDER_STATUS_CHANGED,
    RESERVATION_CREATED,
    RESERVATION_STATUS_CHANGED,
    RESERVATION_CHECKED_IN,
    SPOT_BOARD_CHANGED,
    DASHBOARD_REFRESH
}

public class NotificationEvent extends ApplicationEvent {
    private final NotificationEventType type;
    private final Long merchantId;
    private final Long pondId;
    private final Long resourceId;
    private final Object payload;
    private final LocalDateTime occurAt;
    // static of(...)
}
```

```java
@Component
public class SseEmitterRepository {
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> emitters = ...;
    public SseEmitter register(Long merchantId, String clientId);
    public void remove(Long merchantId, String clientId);
    public Collection<SseEmitter> findByMerchantId(Long merchantId);
    public void sendToMerchant(Long merchantId, NotificationEvent event);
}
```

```java
@RestController
@RequestMapping("/api/merchant/sse")
public class MerchantSseController extends MerchantBaseController {
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam(required = false) String clientId) { ... }
}
```

### 修改文件

| 文件 | 修改点 |
|------|--------|
| `src/main/java/com/example/fishing/interceptor/LoginInterceptor.java` | SSE 无法自定义请求头，需支持从 URL 参数 `token` 读取 JWT |
| `src/main/java/com/example/fishing/service/impl/RestaurantOrderServiceImpl.java` | createOrder / pay / updateStatus 后发布事件 |
| `src/main/java/com/example/fishing/service/impl/ShopOrderServiceImpl.java` | createOrder / pay / updateStatus 后发布事件 |
| `src/main/java/com/example/fishing/service/impl/ReservationServiceImpl.java` | createReservation / doCancel / expirePendingReservations / updateActualFee 后发布事件 |
| `src/main/java/com/example/fishing/service/impl/CheckinServiceImpl.java` | checkin 后发布事件 |
| `src/main/java/com/example/fishing/service/impl/FishingSpotServiceImpl.java` | 影响看板的状态变更后发布事件 |
| `src/main/java/com/example/fishing/service/impl/DrawResultServiceImpl.java` | 抽号完成后发布 SPOT_BOARD_CHANGED / RESERVATION_STATUS_CHANGED |

### 认证适配

`EventSource` 不支持自定义 Header，SSE 连接通过 URL 参数携带 token：

```
GET /api/merchant/sse/connect?token=<jwt>&clientId=<uuid>
```

`LoginInterceptor` 需改为：
1. 优先读取 `Authorization` 头；
2. 不存在时读取 `token` 请求参数；
3. 仍无则返回 401。

### 多实例广播

1. `NotificationEventListener` 监听到事件后，使用 `StringRedisTemplate.convertAndSend("notify:merchant:" + merchantId, json)` 发布；
2. `RedisNotificationListener` 订阅 `notify:merchant:*`（使用 Redis 通配符订阅或按 pattern 订阅）；
3. 每个实例收到后反序列化并调用 `MerchantSseService.sendToMerchant(merchantId, event)`，只推送给本实例维护的 SseEmitter。

### 心跳与超时

- `SseEmitter` 超时设置为 `0L`（永不超时）；
- 每 30 秒发送一条 SSE comment（`:heartbeat\n\n`），防止 Nginx/代理断开；
- 发送失败/IOException 时从仓库移除对应 emitter。

### 事务一致性

事件监听使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`，确保数据库事务提交后再推送，避免商家端刷新时查不到新数据。

## Frontend Design

### 新增文件

| 项目 | 文件 | 说明 |
|------|------|------|
| fishing-pc | `src/utils/sse.ts` | MerchantSseClient 封装 |
| fishing-pc | `src/composables/useMerchantPush.ts` | 组合式函数 |
| fishing-admin | `src/utils/sse.ts` | MerchantSseClient 封装 |
| fishing-admin | `src/composables/useMerchantPush.ts` | 组合式函数 |

### 核心类设计

```ts
class MerchantSseClient {
  private es: EventSource | null = null
  private url: string
  private reconnectDelay = 1000
  private maxReconnectDelay = 30000
  private errorCount = 0
  private listeners = new Map<string, Set<(data: any) => void>>()
  private fallbackTimer: number | null = null

  constructor(baseUrl: string, token: string, clientId: string) {
    this.url = `${baseUrl}/api/merchant/sse/connect?token=${token}&clientId=${clientId}`
  }

  connect(): void
  close(): void
  on(eventType: string, cb: (data: any) => void): () => void
  off(eventType: string, cb: (data: any) => void): void
  enableFallback(intervalMs: number, cb: () => void): void
  disableFallback(): void
  private reconnect(): void
}
```

### 组合式函数

```ts
export function useMerchantPush(options: {
  events: string[]
  onEvent: (type: string, data: any) => void
  fallback: () => void
}) {
  let client: MerchantSseClient | null = null
  onMounted(() => {
    const userStore = useUserStore()
    if (!userStore.token) return
    client = new MerchantSseClient('', userStore.token, uuid())
    options.events.forEach(evt =>
      client!.on(evt, data => options.onEvent(evt, data))
    )
    client.connect()
    client.enableFallback(30000, options.fallback)
  })
  onUnmounted(() => { client?.disableFallback(); client?.close() })
}
```

### 页面接入

在以下页面引入 `useMerchantPush`，收到事件后调用现有 `load()` 或 `refresh()`：

| 项目 | 页面 | 监听事件 |
|------|------|----------|
| fishing-pc | `src/views/merchant/MerchantRestaurantOrders.vue` | `RESTAURANT_ORDER_CREATED`, `RESTAURANT_ORDER_STATUS_CHANGED` |
| fishing-pc | `src/views/merchant/MerchantReservations.vue` | `RESERVATION_CREATED`, `RESERVATION_STATUS_CHANGED`, `RESERVATION_CHECKED_IN` |
| fishing-pc | `src/views/merchant/MerchantDashboard.vue` | `DASHBOARD_REFRESH`, `RESERVATION_CREATED`, `RESERVATION_CHECKED_IN`, `RESTAURANT_ORDER_CREATED` |
| fishing-pc | `src/views/merchant/MerchantPondBoard.vue` | `SPOT_BOARD_CHANGED`, `RESERVATION_STATUS_CHANGED`, `RESERVATION_CHECKED_IN` |
| fishing-pc | `src/views/merchant/MerchantShopProducts.vue` / ShopOrders | `SHOP_ORDER_CREATED`, `SHOP_ORDER_STATUS_CHANGED` |
| fishing-admin | 对应商家端页面 | 同上 |

接入后移除原有 `setInterval` 轮询代码（如 `MerchantRestaurantOrders.vue` 中的 30 秒轮询），由 SSE 触发刷新；SSE 不可用时启用兜底轮询。

### 兜底策略

- SSE 连续失败 3 次后自动启用 30 秒轮询；
- SSE 重连成功后立即关闭轮询；
- 页面切回前台时立即尝试重连并刷新一次；
- 指数退避重连：1s → 2s → 4s → ... → 30s。

## Implementation Steps

1. **新增事件模型**：`NotificationEventType`、`NotificationEvent`、`NotificationPayload`。
2. **实现 SSE 管理层**：`SseEmitterRepository`、`MerchantSseService`、`MerchantSseController`。
3. **适配 JWT**：修改 `LoginInterceptor` 支持 `token` URL 参数。
4. **实现 Redis 跨实例广播**：`RedisNotificationBridge` + `RedisNotificationListener`。
5. **增加本地事件监听**：`NotificationEventListener`（使用 AFTER_COMMIT）。
6. **业务埋点**：
   - 餐厅订单 Service
   - 商城订单 Service
   - 预约 Service
   - 核销 Service
   - 钓位/抽号 Service
7. **前端封装**：两个项目分别新增 `sse.ts` 和 `useMerchantPush.ts`。
8. **页面接入**：餐厅订单、预约、工作台、钓位看板、商城订单页面接入 SSE 并移除旧轮询。
9. **联调验证**：
   - 用户端提交后商家端立即刷新；
   - 多标签页同时收到；
   - 断网重连；
   - 多实例 Redis 广播。

## Verification

1. 启动后端和 Redis；
2. 商家后台打开“餐厅订单”页面；
3. 用户端（PC/H5）提交餐厅订单；
4. 观察商家端餐厅订单列表是否自动新增该订单（1-2 秒内）；
5. 商家后台打开“预约管理”页面；
6. 用户端提交预约；
7. 观察预约列表是否自动刷新；
8. 打开多个商家标签页/不同浏览器，验证都能收到；
9. 断开网络 10 秒后恢复，验证自动重连并刷新；
10. 若部署两个后端实例，验证 Redis 广播后两实例上的商家页面都能收到。

## Critical Files

- `src/main/java/com/example/fishing/notify/MerchantSseController.java`
- `src/main/java/com/example/fishing/notify/MerchantSseServiceImpl.java`
- `src/main/java/com/example/fishing/notify/SseEmitterRepository.java`
- `src/main/java/com/example/fishing/notify/NotificationEvent.java`
- `src/main/java/com/example/fishing/notify/RedisNotificationBridge.java`
- `src/main/java/com/example/fishing/notify/RedisNotificationListener.java`
- `src/main/java/com/example/fishing/interceptor/LoginInterceptor.java`
- `src/main/java/com/example/fishing/service/impl/RestaurantOrderServiceImpl.java`
- `src/main/java/com/example/fishing/service/impl/ShopOrderServiceImpl.java`
- `src/main/java/com/example/fishing/service/impl/ReservationServiceImpl.java`
- `src/main/java/com/example/fishing/service/impl/CheckinServiceImpl.java`
- `fishing-pc/src/utils/sse.ts`
- `fishing-pc/src/composables/useMerchantPush.ts`
- `fishing-admin/src/utils/sse.ts`
- `fishing-admin/src/composables/useMerchantPush.ts`
- `fishing-pc/src/views/merchant/MerchantRestaurantOrders.vue`
- `fishing-pc/src/views/merchant/MerchantReservations.vue`
- `fishing-admin/src/views/merchant/MerchantRestaurantOrders.vue`
- `fishing-admin/src/views/merchant/MerchantReservations.vue`
