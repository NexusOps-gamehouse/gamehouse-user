package gg.duo.user.domain.gameprofile;

import gg.duo.common.constant.GameCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserGameProfileRepository extends JpaRepository<UserGameProfile, Long> {

    List<UserGameProfile> findByUserId(Long userId);

    Optional<UserGameProfile> findByUserIdAndGameCode(Long userId, GameCode gameCode);

    /** 목록 변환용 — 사용자마다 한 번씩 부르는 대신 id 를 모아 한 번에 가져간다. */
    List<UserGameProfile> findByUserIdIn(Collection<Long> userIds);
}
