package gg.duo.user.domain.survey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 성향 설문 응답과 그 환산 결과.
 *
 * 원본 응답(answers)과 환산 점수를 같이 남기는 이유: 환산 공식은 앞으로 바뀐다.
 * 원본을 버리면 공식이 바뀔 때 전원에게 설문을 다시 받아야 한다. 원본이 있으면
 * 재환산 배치 한 번으로 끝난다.
 *
 * [FR-01] 축 구성이 6개(공격성·소통성·승부욕·유연성·인내심·몰입도)에서
 * 7개로 바뀌었다. 설문 6영역 중 '주도성'만 축을 둘로 나눈다. (PlayStyleAxis 참고)
 * 컬럼이 바뀌었으므로 scoringVersion 을 2 로 올린다 — 1로 저장된 행은 옛 공식의
 * 결과라 지금 매칭에 섞으면 안 되고, 재환산 대상을 이 값으로 고른다.
 */
@Entity
@Table(name = "play_style_surveys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PlayStyleSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 12문항 원본 응답, 콤마 구분 (예: "3,5,1,4,...") */
    @Column(nullable = false, length = 100)
    private String answers;

    // 7축 환산 점수 (0~100)
    private Integer winOrientation;
    private Integer mistakeTolerance;
    private Integer communication;
    private Integer focus;
    /** 9번 — 내가 방향을 제시하는 정도 */
    private Integer initiative;
    /** 10번 — 상대가 이끌어주기를 바라는 정도. initiative 와 짝이지 평균 낼 값이 아니다. */
    private Integer initiativePreference;
    private Integer sociality;

    /** 환산에 쓴 공식의 버전. 공식이 바뀌면 올리고, 재환산 대상을 이걸로 고른다. */
    @Column(nullable = false)
    private int scoringVersion = 2;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
