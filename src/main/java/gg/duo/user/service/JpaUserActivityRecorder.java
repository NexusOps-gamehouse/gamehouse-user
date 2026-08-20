package gg.duo.user.service;

import gg.duo.common.security.UserActivityRecorder;
import gg.duo.user.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 마지막 활동 시각 기록 — user 서비스에만 존재하는 구현.
 *
 * common 의 JwtAuthFilter 가 ObjectProvider 로 이 빈을 찾는다.
 * post·chat·riot 에는 이 클래스가 없으므로 필터는 아무 일도 하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JpaUserActivityRecorder implements UserActivityRecorder {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void recordActive(Long userId) {
        userRepository.findById(userId).ifPresent(u -> u.setLastActiveAt(Instant.now()));
    }
}
