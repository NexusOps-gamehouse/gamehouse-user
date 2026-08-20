package gg.duo.user.domain.survey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayStyleSurveyRepository extends JpaRepository<PlayStyleSurvey, Long> {

    Optional<PlayStyleSurvey> findByUserId(Long userId);

    /** 매칭 점수 계산용 — 팀원들의 성향을 한 번에 가져간다. */
    List<PlayStyleSurvey> findByUserIdIn(Collection<Long> userIds);
}
