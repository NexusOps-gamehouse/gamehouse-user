package gg.duo.user.event.publisher;

import gg.duo.common.event.DomainEventPublisher;
import gg.duo.common.event.UserProfileUpdatedEvent;
import gg.duo.user.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 프로필 변경 알림.
 *
 * 닉네임·프로필 이미지를 복제해 둔 서비스(chat 의 메시지 발신자 스냅샷 등)가
 * 이 이벤트를 받아 자기 사본을 갱신한다.
 */
@Component
@RequiredArgsConstructor
public class UserProfileUpdatedPublisher {

    private final DomainEventPublisher publisher;

    public void publish(User user) {
        publisher.publish(new UserProfileUpdatedEvent(
                user.getId(), user.getNickname(), user.getProfileImageUrl()));
    }
}
