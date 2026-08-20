package gg.duo.user.domain.survey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 성향 설문 응답과 그 환산 결과.
 *
 * 원본 응답(answers)과 환산 점수(6축)를 같이 남기는 이유: 환산 공식은 앞으로
 * 바뀐다. 원본을 버리면 공식이 바뀔 때 전원에게 설문을 다시 받아야 한다.
 * 원본이 있으면 재환산 배치 한 번으로 끝난다.
 *
 * ★ 1단계에서는 스키마와 클래스만 만든다. 환산 로직은 SurveyService 의 TODO.
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

    // 6축 환산 점수 (0~100)
    private Integer aggression;
    private Integer communication;
    private Integer competitiveness;
    private Integer flexibility;
    private Integer patience;
    private Integer commitment;

    /** 환산에 쓴 공식의 버전. 공식이 바뀌면 올리고, 재환산 대상을 이걸로 고른다. */
    @Column(nullable = false)
    private int scoringVersion = 1;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
