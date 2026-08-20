package gg.duo.user.controller;

import gg.duo.user.dto.SurveyDto;
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

    /** 성향 설문 제출 (12문항) */
    @PostMapping
    public SurveyDto submit(@RequestBody SurveyDto.SubmitRequest req, Authentication auth) {
        return surveyService.submit((Long) auth.getPrincipal(), req.answers());
    }
}
