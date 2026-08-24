package gg.duo.user.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    private String profileImageUrl;

    // 본인 확인용 필드
    private String name;
    private String phone;

    // 프로필 정보
    //
    // [FR-01] gender 는 민감정보라 더 이상 수집·보관하지 않는다.
    //   컬럼 자체를 지우려면 별도 마이그레이션이 필요하다(ddl-auto: update 는 컬럼을
    //   드롭하지 않는다). db/migration/V3__survey_revamp.sql 참고.
    // [FR-01] ageRange('20대') → age(숫자). 구간으로 받으면 '19세와 20세'가 다른 칸에
    //   들어가고 '20세와 29세'가 같은 칸에 들어간다. 매칭에 쓸 수 있는 형태가 아니다.
    private Integer age;
    private String game;
    private String playStyle;
    private String position;
    private boolean mic;
    private String tier;
    private String playTimes;
    /** 주로 플레이하는 요일, 콤마 구분. ("월,수,금" 또는 "상관없음") */
    private String playDays;
    /** 1회 플레이 선호 분량. ("2~4시간") */
    private String playDuration;
    private String gameModes;
    private String riotNickname;

    // 라이엇 연동 정보
    private String puuid;
    private String gameName;
    private String tagLine;

    // 라이엇 프로필 스냅샷 — "다시 불러오기"를 누른 시점의 값을 그대로 보관한다.
    //
    // 예전에는 puuid / gameName / tagLine 만 저장하고 나머지는 응답으로만 흘려보냈다.
    // 그래서 마이페이지를 떠났다 돌아오면 화면에서 값이 사라졌다.
    // 여기에 남겨두면 페이지를 옮겨도, 새로고침해도, 다른 기기에서도 같은 값이 보이고,
    // 라이엇 API 를 다시 부르지 않으므로 레이트 리밋(2분 100회)도 아낀다.
    //
    // 위쪽 tier 는 회원가입 설문에서 사용자가 직접 고른 한글 값이라 의미가 다르다.
    // 섞이지 않도록 라이엇에서 받은 값에는 riot 접두어를 붙였다.
    private Integer profileIconId;
    private Long summonerLevel;
    private String riotTier;        // 라이엇 enum. 예: DIAMOND
    private String riotRank;        // 세부 등급. 예: III
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private Instant riotSyncedAt;   // 마지막으로 갱신한 시각

    private Instant lastActiveAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now(); //..
}