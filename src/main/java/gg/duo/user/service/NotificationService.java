package gg.duo.user.service;

import gg.duo.user.domain.notification.Notification;
import gg.duo.user.domain.notification.NotificationRepository;
import gg.duo.user.domain.user.User;
import gg.duo.user.domain.user.UserRepository;
import gg.duo.user.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 알림.
 *
 * notifications 테이블은 user 서비스가 소유한다. post·chat 은 여기에 직접
 * INSERT 할 수 없고 NotificationRequestedEvent 를 발행한다
 * (→ NotificationRequestedConsumer 가 notify(Long, ...) 를 호출한다).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** 서비스 내부용 — 이미 User 를 들고 있을 때. */
    @Transactional
    public void notify(User recipient, String message, String link) {
        Notification n = new Notification();
        n.setUser(recipient);
        n.setMessage(message);
        n.setLink(link);
        notificationRepository.save(n);
    }

    /**
     * 다른 서비스에서 들어온 알림 요청.
     *
     * 수신자를 못 찾으면 조용히 버린다. 이벤트는 이미 발생한 사실이라
     * 되돌릴 수 없고, 여기서 예외를 던지면 발행한 쪽의 작업까지 말려든다.
     */
    @Transactional
    public void notify(Long userId, String message, String link) {
        userRepository.findById(userId)
                .ifPresent(u -> notify(u, message, link));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId) {
        List<NotificationDto> items = notificationRepository
                .findTop30ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationDto::from).toList();
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return Map.of("items", items, "unreadCount", unread);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdAndIsReadFalse(userId)
                .forEach(n -> n.setRead(true));
    }
}
