package gg.duo.user.domain.mastery;

import gg.duo.user.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_champion_masteries")
public class UserChampionMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String game;         // 게임 종류 (LOL, VALORANT 등)
    private Long championId;  // 챔피언 ID (Integer에서 Long으로 수정)
    private Integer masteryLevel;// 숙련도 레벨
    private Integer masteryPoints;// 숙련도 점수
    private Integer ranking;     // 숙련도 순위 (1~3)

    private Instant syncedAt;    // 마지막 동기화 시간
}