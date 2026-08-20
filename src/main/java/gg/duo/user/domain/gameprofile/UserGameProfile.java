package gg.duo.user.domain.gameprofile;

import gg.duo.common.constant.GameCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 게임별 플레이 프로필. 멀티게임의 열쇠다.
 *
 * 지금 User 에는 game / position / tier / gameModes 가 한 벌씩만 있다.
 * 그래서 "롤도 하고 발로란트도 함"을 표현할 수 없고, 발로란트 정보를 넣으려면
 * 롤 정보를 덮어써야 한다.
 *
 * ⚠️ 1단계에서는 User 의 기존 필드를 그대로 두고 이 테이블을 병행 신설한다.
 *    옮기는 순간 프론트 응답(UserDto.game/position/tier)과 회원가입 설문이
 *    같이 바뀌어야 하는데, 그건 구조 변경이 아니라 기능 변경이다.
 *    마이그레이션은 별도 PR 로 뺀다.
 *
 * role 을 enum 이 아니라 String 으로 두는 이유: 포지션 체계가 게임마다 다르다.
 * (롤 = 탑/정글/미드/원딜/서폿, 발로란트 = 듀얼리스트/컨트롤러/이니시에이터/센티널)
 * 공통 enum 으로 묶으면 게임이 늘 때마다 common 을 고쳐 전체를 재배포해야 한다.
 */
@Entity
@Table(name = "user_game_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGameProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * users.id.
     *
     * @ManyToOne User 로 두지 않는 이유는 없다 — 같은 user 서비스 안이라
     * 연관을 걸어도 경계를 넘지 않는다. 다만 이 테이블은 앞으로 match 서비스가
     * 가장 자주 읽을 대상이라, 조회 경로를 id 하나로 단순하게 유지한다.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_code", nullable = false, length = 32)
    private GameCode gameCode;

    /** 주 포지션. 게임마다 체계가 다르므로 문자열이다. */
    private String role;

    private String tier;
    private String rankDivision;
    private Integer leaguePoints;

    /** 선호 게임 모드, 콤마 구분 (예: "랭크,일반") */
    private String gameModes;

    /**
     * 게임별 추가 필드.
     *
     * 발로란트에는 있고 롤에는 없는 값(에이전트 선호 등)을 컬럼으로 계속 늘리면
     * 게임이 추가될 때마다 스키마가 바뀐다. 공통이 아닌 것은 여기 넣는다.
     */
    @Column(columnDefinition = "text")
    private String extra;

    /**
     * @Builder.Default 가 없으면 lombok 이 이 초기화식을 무시한다.
     * 빌더로 만들 때 createdAt 을 빼먹으면 null 이 들어가고, nullable=false 라
     * INSERT 단계에서 터진다. 지금은 GameProfileService 가 명시적으로 넣고 있지만
     * 호출처가 하나 늘어나는 순간 놓치기 쉬운 자리다.
     */
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
