package gg.duo.user.dto;

/** riot 서비스가 돌려주는 챔피언 숙련도. user 가 자기 형태로 정의한 사본이다. */
public record ChampionMasteryView(
        Integer ranking,
        Long championId,
        Integer championMasteryLevel,
        Integer championMasteryPoints
) {}
