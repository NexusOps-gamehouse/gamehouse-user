package gg.duo.user.service;

import gg.duo.user.domain.survey.PlayStyleSurvey;
import gg.duo.user.domain.survey.PlayStyleSurveyRepository;
import gg.duo.user.dto.SurveyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 성향 설문 — 12문항을 6축으로 환산해 저장한다.
 *
 * ★ 1단계 범위: 구조와 저장까지. 환산 공식은 아직 정해지지 않아 TODO 로 둔다.
 *   지금은 원본 응답만 저장하고 점수는 비워둔다. 원본이 남아 있으면 공식이
 *   정해진 뒤 재환산 배치 한 번으로 전원 채울 수 있다.
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    /** 설문 문항 수. 바뀌면 SCORING_VERSION 도 올린다. */
    public static final int QUESTION_COUNT = 12;

    private static final int SCORING_VERSION = 1;

    private final PlayStyleSurveyRepository surveyRepository;

    @Transactional
    public SurveyDto submit(Long userId, List<Integer> answers) {
        if (answers == null || answers.size() != QUESTION_COUNT)
            throw new IllegalArgumentException(QUESTION_COUNT + "개 문항에 모두 응답해주세요.");

        PlayStyleSurvey survey = surveyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PlayStyleSurvey s = new PlayStyleSurvey();
                    s.setUserId(userId);
                    return s;
                });

        survey.setAnswers(answers.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse(""));
        survey.setScoringVersion(SCORING_VERSION);

        // TODO(2단계): 12문항 → 6축 환산.
        //   축마다 어떤 문항이 기여하는지(가중치 표)가 먼저 정해져야 한다.
        //   정해지기 전에 임의 공식을 넣으면 그 값으로 매칭이 돌아가 버리고,
        //   나중에 바꿀 때 "왜 추천이 달라졌지"를 설명할 수 없다.

        surveyRepository.save(survey);
        return toDto(survey);
    }

    @Transactional(readOnly = true)
    public SurveyDto get(Long userId) {
        return surveyRepository.findByUserId(userId).map(this::toDto).orElse(null);
    }

    private SurveyDto toDto(PlayStyleSurvey s) {
        List<Integer> answers = s.getAnswers() == null || s.getAnswers().isBlank()
                ? List.of()
                : java.util.Arrays.stream(s.getAnswers().split(",")).map(Integer::parseInt).toList();

        java.util.Map<String, Integer> scores = new java.util.LinkedHashMap<>();
        scores.put("aggression", s.getAggression());
        scores.put("communication", s.getCommunication());
        scores.put("competitiveness", s.getCompetitiveness());
        scores.put("flexibility", s.getFlexibility());
        scores.put("patience", s.getPatience());
        scores.put("commitment", s.getCommitment());

        return new SurveyDto(s.getUserId(), answers, scores, s.getScoringVersion());
    }
}
