package gg.duo.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * riot 서비스 응답에 대한 user 쪽 계약.
 *
 * riot 모듈의 RiotProfileResponseDTO 를 직접 import 하지 않는다. import 하는 순간
 * user 가 riot 의 클래스에 컴파일 의존하게 되고, riot 이 필드를 하나 바꾸면
 * user 를 다시 빌드·배포해야 한다. 서비스를 나눈 이유가 사라진다.
 *
 * 대신 같은 JSON 을 각자 자기 형태로 읽는다. @JsonIgnoreProperties 를 켜 두면
 * riot 이 필드를 추가해도 user 는 무시하고 넘어간다 (하위 호환).
 *
 * 필드 이름은 기존 응답과 동일하다 — 프론트(api/riot.js)가 그대로 동작해야 한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiotProfileView(
        String puuid,
        String gameName,
        String tagLine,
        Integer profileIconId,
        Long summonerLevel,
        String tier,
        String rank,
        Integer leaguePoints,
        Integer wins,
        Integer losses,
        List<ChampionMasteryView> championMasteries,
        /**
         * 마지막으로 라이엇을 실제 호출해 갱신한 시각.
         * 프론트가 쿨다운 남은 시간을 계산하는 데 쓴다.
         */
        Instant riotSyncedAt
) {
    public RiotProfileView withSyncedAt(Instant syncedAt) {
        return new RiotProfileView(puuid, gameName, tagLine, profileIconId, summonerLevel,
                tier, rank, leaguePoints, wins, losses, championMasteries, syncedAt);
    }
}
