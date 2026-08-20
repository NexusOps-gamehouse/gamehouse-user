package gg.duo.user.dto;

import java.util.List;
import java.util.Map;

/** 성향 설문 요청·응답. */
public record SurveyDto(
        Long userId,
        List<Integer> answers,
        /** 축 이름 → 0~100 점수 */
        Map<String, Integer> scores,
        int scoringVersion
) {

    /** 제출 요청 — 12문항 응답만 받는다. */
    public record SubmitRequest(List<Integer> answers) {}
}
