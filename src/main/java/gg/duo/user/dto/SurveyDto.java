package gg.duo.user.dto;

import gg.duo.user.domain.survey.PlayStyleAxis;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 성향 설문 요청·응답. */
public record SurveyDto(
        Long userId,
        List<Integer> answers,
        /** 축 이름(WIN_ORIENTATION 등) → 0~100 점수 */
        Map<String, Integer> scores,
        int scoringVersion
) {

    /** 제출 요청 — 12문항 응답만 받는다. */
    public record SubmitRequest(List<Integer> answers) {}

    /**
     * 축 메타데이터. 프론트가 결과를 그릴 때 축 이름·설명·문항 구성을 물어본다.
     *
     * 문구를 프론트에 복사해두지 않는 이유: 축이 바뀌면 두 곳이 어긋나고,
     * 어긋난 채로 배포되면 화면은 '승부욕'인데 값은 '승리 지향성'이 된다.
     */
    public record AxisView(String key, String displayName, String description, List<Integer> questions) {

        public static List<AxisView> all() {
            return Arrays.stream(PlayStyleAxis.values())
                    .map(a -> new AxisView(a.name(), a.displayName(), a.description(), a.questions()))
                    .toList();
        }
    }

    /** 축 → 점수 맵을 축 선언 순서대로 만든다. (LinkedHashMap: 화면 순서가 매번 바뀌지 않게) */
    public static Map<String, Integer> emptyScores() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (PlayStyleAxis axis : PlayStyleAxis.values()) m.put(axis.name(), null);
        return m;
    }
}
