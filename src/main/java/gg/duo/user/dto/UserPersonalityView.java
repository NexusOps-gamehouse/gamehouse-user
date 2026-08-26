package gg.duo.user.dto;

/**
 * 성향 축 점수 — 서비스 간 내부 호출 전용 응답.
 *
 * UserDto(공개)에는 이 값을 넣지 않는다. UserDto는 /api/users/{id}, /api/posts
 * 응답에 그대로 실려 로그인한 아무 사용자나 볼 수 있는데, 성향 점수는 매칭 계산에만
 * 필요하지 "남의 프로필 조회"에 필요한 값이 아니다. 그래서 /internal/** 로만 내려주는
 * 별도 타입을 둔다 — InternalUserController 참고 (Ingress 밖에서는 안 닿는다).
 *
 * personality가 nested record인 이유: match 서비스(JsonParsingUtils.parsePersonality)가
 * 이미 "personality" 키 아래에서 축 7개를 파싱하도록 짜여 있다. 여기서 같은 모양으로
 * 내려주면 match 쪽 파싱 로직을 그대로 재사용할 수 있다.
 */
public record UserPersonalityView(Long userId, Personality personality) {

    /**
     * 필드명은 PlayStyleAxis(user 서비스 내부 이름)가 아니라 match 서비스가 쓰는
     * 이름을 따른다 — winOrientation→winIntent, initiative→leadership,
     * initiativePreference→leadershipPreference, sociality→sociability.
     * (PersonalityProfile.java, PlayStyleAxis.java 주석 참고)
     */
    public record Personality(
            Integer winIntent,
            Integer mistakeTolerance,
            Integer communication,
            Integer focus,
            Integer leadership,
            Integer leadershipPreference,
            Integer sociability
    ) {}
}
