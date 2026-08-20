package gg.duo.user.controller;

import gg.duo.user.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.common.dto.UserDto;
import gg.duo.user.dto.RiotProfileView;
import gg.duo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        return userService.me((Long) auth.getPrincipal());
    }

    @PutMapping("/me")
    public UserDto update(Authentication auth, @RequestBody ProfileUpdateRequest req) {
        return userService.updateProfile((Long) auth.getPrincipal(), req);
    }

    /** 타 유저 프로필 조회 */
    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    /** [이메일 찾기] 이름 + 전화번호 기준 */
    @GetMapping("/find-email")
    public ResponseEntity<Map<String, String>> findEmail(@RequestParam String name,
                                                         @RequestParam String phone) {
        String maskedEmail = userService.findEmailByNameAndPhone(name, phone);
        return ResponseEntity.ok(Map.of("email", maskedEmail));
    }

    /** 비밀번호 재설정 */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.email(), req.name(), req.phone(),
                req.newPassword(), req.newPasswordConfirm());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }

    public record ResetPasswordRequest(
            String email,
            String name,
            String phone,
            String newPassword,
            String newPasswordConfirm
    ) {}

    /** 라이엇 프로필 동기화 */
    /**
     * 라이엇 계정 연동.
     *
     * 반환 타입이 UserDto 였는데, 프론트(api/riot.js normalizeProfile)는
     * RiotProfileView 형태(tier / rank / leaguePoints / championMasteries)를
     * 기대한다. UserDto 에는 그 필드들이 없어 leaguePoints 는 0,
     * championMasteries 는 없음으로 처리되고 있었다.
     * 또 UserDto.tier 는 사용자가 직접 고른 한글 값이라 라이엇 티어처럼 보였다.
     */
    @PostMapping("/riot/sync")
    public ResponseEntity<RiotProfileView> syncRiotProfile(
            Authentication authentication,
            @RequestBody RiotSyncRequestDTO request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        RiotProfileView response = userService.syncRiotProfile(
                userId,
                request.gameName(),
                request.tagLine()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 저장해 둔 라이엇 프로필 조회.
     *
     * 마이페이지에 들어올 때마다 호출되는 자리라 라이엇 API 를 부르지 않는다.
     * DB 에 남겨둔 마지막 스냅샷만 돌려주므로 빠르고, 레이트 리밋과 무관하며,
     * 개발용 키가 만료된 상태에서도 지난 값이 그대로 보인다.
     * 실제 갱신은 사용자가 "다시 불러오기"를 눌러 POST /riot/sync 를 호출할 때만 일어난다.
     *
     * 아직 연동하지 않았으면 204 No Content.
     */
    @GetMapping("/riot/profile")
    public ResponseEntity<RiotProfileView> storedRiotProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        RiotProfileView profile = userService.storedRiotProfile(userId);
        return profile == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(profile);
    }

    public record RiotSyncRequestDTO(
            String gameName,
            String tagLine
    ) {}
}