package gg.duo.user.controller;

import gg.duo.user.dto.SurveyDto;

import java.util.List;
import gg.duo.user.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/survey")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    /** 내 성향 설문 조회 — 아직 안 했으면 204 */
    @GetMapping
    public ResponseEntity<SurveyDto> get(Authentication auth) {
        SurveyDto survey = surveyService.get((Long) auth.getPrincipal());
        return survey == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(survey);
    }

    /**
     * 축 메타데이터 — 프론트가 결과 화면(레이더 차트 등)의 축 이름을 여기서 받는다.
     * 인증이 필요 없는 정적 정보지만, 설문과 같은 자원이라 경로를 붙여둔다.
     */
    @GetMapping("/axes")
    public List<SurveyDto.AxisView> axes() {
        return SurveyDto.AxisView.all();
    }

    /** 성향 설문 제출 (12문항) */
    @PostMapping
    public SurveyDto submit(@RequestBody SurveyDto.SubmitRequest req, Authentication auth) {
        return surveyService.submit((Long) auth.getPrincipal(), req.answers());
    }
}
