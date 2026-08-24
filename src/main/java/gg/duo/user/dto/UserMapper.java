package gg.duo.user.dto;

import gg.duo.common.dto.UserDto;
import gg.duo.user.domain.user.User;

import java.time.Duration;
import java.time.Instant;

/**
 * User 엔티티 → 공개 뷰(UserDto) 변환.
 *
 * 변환 규칙이 user 서비스에 있는 이유: UserDto 는 common 에 있어야 post·chat 이
 * 참조할 수 있지만, User 엔티티는 user 만 볼 수 있어야 한다. common 이 엔티티를
 * 알면 모든 서비스가 users 테이블에 딸려 들어간다.
 *
 * 그래서 "형태"는 common 에, "채우는 법"은 소유자인 user 에 둔다.
 */
public final class UserMapper {

    private UserMapper() {}

    /** 최근 5분 안에 활동했으면 온라인으로 본다. */
    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    public static UserDto from(User u) {
        boolean online = u.getLastActiveAt() != null
                && u.getLastActiveAt().isAfter(Instant.now().minus(ONLINE_WINDOW));
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getNickname(),
                u.getProfileImageUrl(),
                u.getAge(),
                u.getGame(),
                u.getPlayStyle(),
                u.getPosition(),
                u.isMic(),
                u.getTier(),
                u.getRiotTier(),
                u.getRiotRank(),
                u.getPlayTimes(),
                u.getPlayDays(),
                u.getPlayDuration(),
                u.getGameModes(),
                u.getRiotNickname(),
                u.getPuuid(),
                u.getGameName(),
                u.getTagLine(),
                online
        );
    }
}
