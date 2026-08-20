package gg.duo.user.service;

import gg.duo.common.constant.GameCode;
import gg.duo.user.domain.gameprofile.UserGameProfile;
import gg.duo.user.domain.gameprofile.UserGameProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 게임별 프로필.
 *
 * ★ 1단계 범위: CRUD 골격까지. User 의 game/position/tier 를 이쪽으로 옮기는
 *   마이그레이션은 별도 PR 이다 (프론트 응답 형태가 같이 바뀐다).
 */
@Service
@RequiredArgsConstructor
public class GameProfileService {

    private final UserGameProfileRepository gameProfileRepository;

    @Transactional(readOnly = true)
    public List<UserGameProfile> myProfiles(Long userId) {
        return gameProfileRepository.findByUserId(userId);
    }

    @Transactional
    public UserGameProfile upsert(Long userId, GameCode gameCode, String role,
                                  String tier, String rankDivision, String gameModes) {
        UserGameProfile profile = gameProfileRepository
                .findByUserIdAndGameCode(userId, gameCode)
                .orElseGet(() -> UserGameProfile.builder()
                        .userId(userId)
                        .gameCode(gameCode)
                        .createdAt(Instant.now())
                        .build());

        profile.setRole(role);
        profile.setTier(tier);
        profile.setRankDivision(rankDivision);
        profile.setGameModes(gameModes);
        profile.setUpdatedAt(Instant.now());

        return gameProfileRepository.save(profile);
    }

    // TODO(2단계): riot 동기화 결과를 LOL 프로필에 자동 반영.
    //   UserService.syncRiotProfile 이 users 컬럼에만 쓰고 있어 이중 관리 중이다.
}
