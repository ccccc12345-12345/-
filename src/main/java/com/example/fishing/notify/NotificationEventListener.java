package com.example.fishing.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 本地通知事件监听器
 *
 * 监听到业务事件后，通过 Redis 广播到所有后端实例。
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private RedisNotificationBridge redisNotificationBridge;

    /**
     * 事务提交后再广播，确保商家端刷新时能查到最新数据。
     *
     * NotificationEvent 为非 ApplicationEvent 的 POJO，Spring 会将其包装为
     * PayloadApplicationEvent 发布，因此这里监听 PayloadApplicationEvent。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationEvent(PayloadApplicationEvent<NotificationEvent> payloadEvent) {
        NotificationEvent event = payloadEvent.getPayload();
        if (event.getMerchantId() == null) {
            log.debug("事件缺少 merchantId，跳过广播: type={}", event.getType());
            return;
        }
        redisNotificationBridge.publish(event);
    }
}
