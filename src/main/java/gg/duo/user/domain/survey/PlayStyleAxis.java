package gg.duo.user.domain.survey;

import java.util.List;

/**
 * 성향 축.
 *
 * 12문항을 그대로 매칭에 쓰지 않고 축으로 환산하는 이유: 문항은 언제든 바뀌지만
 * (문구 수정, 개수 조정) 축은 매칭 점수 공식이 참조하는 계약이다. 둘을 분리해야
 * 설문을 고쳐도 매칭 로직을 건드리지 않는다.
 *
 * [FR-01] 설문은 6영역이지만 축은 7개다.
 *   9번("내가 방향을 제시한다")과 10번("상대가 이끌어주면 좋다")은 같은 '주도성'
 *   영역에 있지만 의미가 반대편이다. 둘을 평균 내면 '이끄는 사람'과 '이끌리고
 *   싶은 사람'이 같은 점수가 되고, 그러면 매칭이 이 둘을 구분하지 못한다.
 *   실제로는 이 조합(한쪽 INITIATIVE 높음 + 다른 쪽 INITIATIVE_PREFERENCE 높음)이
 *   가장 잘 맞는 짝이라 반드시 나눠야 한다. → 유사도가 아니라 상보성으로 계산(FR-05).
 *
 * questions 는 이 축에 기여하는 문항 번호(1-base)다. 음수는 역채점을 뜻한다.
 *   예) FOCUS 의 -8 : 8번("잡담을 즐긴다")은 점수가 높을수록 집중도가 낮다.
 *       그대로 더하면 7번과 8번이 서로를 상쇄해 모두가 가운데 점수를 받는다.
 */
public enum PlayStyleAxis {

    WIN_ORIENTATION("승리 지향성", "과정/재미 중시 ↔ 승리 중시", List.of(1, 2)),
    MISTAKE_TOLERANCE("실수 관용도", "실수에 민감 ↔ 실수에 관대", List.of(3, 4)),
    COMMUNICATION("소통 적극성", "필요한 말만 ↔ 적극적으로 소통", List.of(5, 6)),
    FOCUS("플레이 집중도", "편안한 분위기 ↔ 플레이 집중", List.of(7, -8)),
    INITIATIVE("주도성", "다른 사람을 따름 ↔ 먼저 방향 제시", List.of(9)),
    INITIATIVE_PREFERENCE("주도성 선호", "각자 판단 ↔ 이끌어주는 쪽 선호", List.of(10)),
    SOCIALITY("친목 성향", "게임 자체가 중요 ↔ 관계 형성도 중요", List.of(11, 12));

    private final String displayName;
    private final String description;
    private final List<Integer> questions;

    PlayStyleAxis(String displayName, String description, List<Integer> questions) {
        this.displayName = displayName;
        this.description = description;
        this.questions = questions;
    }

    public String displayName() { return displayName; }

    public String description() { return description; }

    /** 이 축에 기여하는 문항 번호(1-base). 음수는 역채점. */
    public List<Integer> questions() { return questions; }

    /** 응답 배열(0-base)에서 이 축의 0~100 점수를 뽑는다. */
    public int score(List<Integer> answers) {
        double sum = 0;
        for (int q : questions) {
            int raw = answers.get(Math.abs(q) - 1);
            // 역채점: 1↔5, 2↔4. (6 - raw)
            sum += q < 0 ? 6 - raw : raw;
        }
        double avg = sum / questions.size();
        // 1~5 를 0~100 으로. 1점 = 0, 3점 = 50, 5점 = 100.
        return (int) Math.round((avg - 1) / 4 * 100);
    }

    /** 이름으로 축 찾기. 저장된 문자열을 다시 축으로 되돌릴 때 쓴다. */
    public static PlayStyleAxis of(String name) {
        return valueOf(name.toUpperCase());
    }
}
