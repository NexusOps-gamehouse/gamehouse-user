package gg.duo.user.controller;

import gg.duo.common.dto.UserDto;
import gg.duo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 서비스 간 호출 전용 API. Ingress 에 노출하지 않는다.
 *
 * /api/** 와 경로를 분리한 이유:
 *   1) Ingress 규칙이 /api/** 만 밖으로 열기 때문에 /internal/** 은 클러스터
 *      안에서만 닿는다. 경로 하나로 노출 범위가 갈린다.
 *   2) 사용자용 API 와 수명이 다르다. 사용자용은 프론트와 함께 바뀌고,
 *      내부용은 호출하는 서비스와 함께 바뀐다. 섞어두면 한쪽을 고칠 때마다
 *      다른 쪽 호환을 걱정해야 한다.
 *
 * 조회가 전부 '묶음' 단위인 이유: 목록 화면 하나가 글 20개를 그리면 단건
 * API 로는 HTTP 왕복이 20번이다. 같은 프로세스일 때의 N+1 보다 훨씬 비싸다.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    /** id 묶음으로 사용자 조회 */
    @GetMapping
    public List<UserDto> findAllByIds(@RequestParam List<Long> ids) {
        return userService.findAllByIds(ids);
    }

    /** 닉네임 부분 일치 → id 목록 */
    @GetMapping("/ids-by-nickname")
    public List<Long> idsByNickname(@RequestParam String keyword) {
        return userService.findIdsByNicknameContaining(keyword);
    }
}
