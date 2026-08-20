package gg.duo.user.domain.survey;

/**
 * 성향 6축.
 *
 * 12문항을 그대로 매칭에 쓰지 않고 6축으로 환산하는 이유: 문항은 언제든
 * 바뀌지만(문구 수정, 개수 조정) 축은 매칭 점수 공식이 참조하는 계약이다.
 * 둘을 분리해야 설문을 고쳐도 매칭 로직을 건드리지 않는다.
 */
public enum PlayStyleAxis {

    AGGRESSION("공격성", "공격적 ↔ 안정적"),
    COMMUNICATION("소통성", "적극 소통 ↔ 조용함"),
    COMPETITIVENESS("승부욕", "승패 중시 ↔ 재미 중시"),
    FLEXIBILITY("유연성", "포지션 양보 ↔ 주 포지션 고수"),
    PATIENCE("인내심", "피드백 수용 ↔ 감정 표현"),
    COMMITMENT("몰입도", "장시간 ↔ 짧게 자주");

    private final String displayName;
    private final String description;

    PlayStyleAxis(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }

    public String description() { return description; }
}
