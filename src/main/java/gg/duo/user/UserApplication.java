package gg.duo.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * user 서비스 — 인증 · 프로필 · 설문 · 친구 · 알림.
 *
 * 소유 테이블: users, user_game_profiles, play_style_surveys,
 *             user_champion_masteries, friends, notifications
 *
 * scanBasePackages 에 gg.duo.common 을 넣는 이유: JwtAuthFilter,
 * GlobalExceptionHandler, LocalDomainEventPublisher 가 common 에 있다.
 * 넣지 않으면 인증 필터가 등록되지 않아 모든 요청이 비로그인으로 처리된다.
 */
@SpringBootApplication(scanBasePackages = {"gg.duo.user", "gg.duo.common"})
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
