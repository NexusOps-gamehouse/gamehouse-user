package gg.duo.user.event.consumer;

import gg.duo.common.event.NotificationRequestedEvent;
import gg.duo.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 다른 서비스가 요청한 알림을 저장한다.
 *
 * @EventListener 는 1단계(프로세스 내 배달)용이다. 3단계에서 RabbitMQ 로
 * 바꿀 때 이 애노테이션이 @RabbitListener 로 바뀌고, 메서드 본문은 그대로다.
 */
@Component
@RequiredArgsConstructor
public class NotificationRequestedConsumer {

    private final NotificationService notificationService;

    @EventListener
    public void on(NotificationRequestedEvent event) {
        notificationService.notify(event.userId(), event.message(), event.link());
    }
}
