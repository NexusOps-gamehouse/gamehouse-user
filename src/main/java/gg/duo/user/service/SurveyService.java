package gg.duo.user.service;

import gg.duo.user.domain.survey.PlayStyleAxis;
import gg.duo.user.domain.survey.PlayStyleSurvey;
import gg.duo.user.domain.survey.PlayStyleSurveyRepository;
import gg.duo.user.dto.SurveyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 성향 설문 — 12문항을 7축으로 환산해 저장한다.
 *
 * [FR-01] 1단계에서 TODO 로 비워뒀던 환산을 채웠다.
 *
 * 환산 규칙은 딱 세 줄이다.
 *   1) 각 축은 자기 문항들의 평균을 쓴다. (PlayStyleAxis.questions)
 *   2) 방향이 반대인 문항은 역채점한다. (8번)
 *   3) 1~5 평균을 0~100 으로 편다. 1점=0, 3점=50, 5점=100.
 *
 * 가중치를 두지 않은 이유: 아직 "어떤 문항이 궁합을 더 잘 설명하는가"에 대한
 * 데이터가 없다. 근거 없는 가중치를 넣으면 나중에 추천이 달라졌을 때 원인을
 * 설명할 수 없다. 원본 응답을 남겨두므로 데이터가 쌓이면 재환산하면 된다.
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    /** 설문 문항 수. 바뀌면 SCORING_VERSION 도 올린다. */
    public static final int QUESTION_COUNT = 12;

    /** 응답 척도 (1~5). MBTI 식 5점 리커트. */
    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;

    /** 2 = 7축 체계(FR-01). 1은 옛 6축이라 섞으면 안 된다. */
    private static final int SCORING_VERSION = 2;

    private final PlayStyleSurveyRepository surveyRepository;

    @Transactional
    public SurveyDto submit(Long userId, List<Integer> answers) {
        validate(answers);

        PlayStyleSurvey survey = surveyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PlayStyleSurvey s = new PlayStyleSurvey();
                    s.setUserId(userId);
                    return s;
                });

        survey.setAnswers(answers.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse(""));
        survey.setScoringVersion(SCORING_VERSION);
        survey.setUpdatedAt(Instant.now());

        applyScores(survey, answers);

        surveyRepository.save(survey);
        return toDto(survey);
    }

    @Transactional(readOnly = true)
    public SurveyDto get(Long userId) {
        return surveyRepository.findByUserId(userId).map(this::toDto).orElse(null);
    }

    /**
     * 응답 검증.
     *
     * 개수만 보고 범위를 보지 않으면 0 이나 7 이 그대로 들어와 점수가 0~100 을
     * 벗어난다. 매칭 쪽은 0~100 을 전제로 거리를 재므로 거기서 조용히 틀린다.
     */
    private void validate(List<Integer> answers) {
        if (answers == null || answers.size() != QUESTION_COUNT)
            throw new IllegalArgumentException(QUESTION_COUNT + "개 문항에 모두 응답해주세요.");

        for (int i = 0; i < answers.size(); i++) {
            Integer a = answers.get(i);
            if (a == null || a < MIN_SCORE || a > MAX_SCORE)
                throw new IllegalArgumentException(
                        (i + 1) + "번 문항의 응답이 올바르지 않습니다. (" + MIN_SCORE + "~" + MAX_SCORE + ")");
        }
    }

    /** 12문항 → 7축. 축 정의(PlayStyleAxis)가 계산을 갖고 있어 여기서는 옮겨 담기만 한다. */
    private void applyScores(PlayStyleSurvey survey, List<Integer> answers) {
        survey.setWinOrientation(PlayStyleAxis.WIN_ORIENTATION.score(answers));
        survey.setMistakeTolerance(PlayStyleAxis.MISTAKE_TOLERANCE.score(answers));
        survey.setCommunication(PlayStyleAxis.COMMUNICATION.score(answers));
        survey.setFocus(PlayStyleAxis.FOCUS.score(answers));
        survey.setInitiative(PlayStyleAxis.INITIATIVE.score(answers));
        survey.setInitiativePreference(PlayStyleAxis.INITIATIVE_PREFERENCE.score(answers));
        survey.setSociality(PlayStyleAxis.SOCIALITY.score(answers));
    }

    private SurveyDto toDto(PlayStyleSurvey s) {
        List<Integer> answers = s.getAnswers() == null || s.getAnswers().isBlank()
                ? List.of()
                : Arrays.stream(s.getAnswers().split(",")).map(String::trim).map(Integer::parseInt).toList();

        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(PlayStyleAxis.WIN_ORIENTATION.name(), s.getWinOrientation());
        scores.put(PlayStyleAxis.MISTAKE_TOLERANCE.name(), s.getMistakeTolerance());
        scores.put(PlayStyleAxis.COMMUNICATION.name(), s.getCommunication());
        scores.put(PlayStyleAxis.FOCUS.name(), s.getFocus());
        scores.put(PlayStyleAxis.INITIATIVE.name(), s.getInitiative());
        scores.put(PlayStyleAxis.INITIATIVE_PREFERENCE.name(), s.getInitiativePreference());
        scores.put(PlayStyleAxis.SOCIALITY.name(), s.getSociality());

        return new SurveyDto(s.getUserId(), answers, scores, s.getScoringVersion());
    }
}
