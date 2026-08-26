package gg.duo.user.domain.survey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayStyleSurveyRepository extends JpaRepository<PlayStyleSurvey, Long> {

    Optional<PlayStyleSurvey> findByUserId(Long userId);

    /** 매칭 점수 계산용 — 팀원들의 성향을 한 번에 가져간다. */
    List<PlayStyleSurvey> findByUserIdIn(Collection<Long> userIds);

    /**
     * 매칭 점수 계산용 — 지정한 채점 버전 이상만 가져온다.
     *
     * SurveyService 가 "1 은 옛 6축이라 섞으면 안 된다"고 못 박아 뒀는데도 조회는
     * 버전을 보지 않고 있었다. v1 행은 initiative / initiativePreference 가 비어 있어
     * 그대로 넘기면 15점짜리 주도성 축이 조용히 중립값으로 계산된다. 여기서 걸러
     * "설문 미응답"과 같은 취급을 받게 한다 — TeamFitCalculator 는 모르는 축을
     * 평균에서 아예 빼므로, 빠지는 편이 잘못된 값이 섞이는 것보다 정확하다.
     */
    List<PlayStyleSurvey> findByUserIdInAndScoringVersionGreaterThanEqual(
            Collection<Long> userIds, int minScoringVersion);
}
