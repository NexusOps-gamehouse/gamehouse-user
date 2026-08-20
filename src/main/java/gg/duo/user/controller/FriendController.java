package gg.duo.user.controller;

import gg.duo.user.dto.FriendDto;
import gg.duo.user.service.FriendService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    public record RequestForm(@NotNull Long receiverId) {}

    /** 친구 신청 */
    @PostMapping("/requests")
    public FriendDto request(@Valid @RequestBody RequestForm form, Authentication auth) {
        return friendService.request(form.receiverId(), (Long) auth.getPrincipal());
    }

    /** 받은/보낸 대기 중인 신청 목록 */
    @GetMapping("/requests")
    public Map<String, List<FriendDto>> pendingRequests(Authentication auth) {
        return friendService.pendingRequests((Long) auth.getPrincipal());
    }

    /** 친구 신청 수락 */
    @PostMapping("/requests/{id}/accept")
    public FriendDto accept(@PathVariable Long id, Authentication auth) {
        return friendService.accept(id, (Long) auth.getPrincipal());
    }

    /** 친구 신청 거절(받은 사람) / 취소(보낸 사람) */
    @DeleteMapping("/requests/{id}")
    public void deleteRequest(@PathVariable Long id, Authentication auth) {
        friendService.deleteRequest(id, (Long) auth.getPrincipal());
    }

    /** 내 친구 목록 */
    @GetMapping
    public List<FriendDto> friends(Authentication auth) {
        return friendService.friends((Long) auth.getPrincipal());
    }

    /** 친구 삭제 */
    @DeleteMapping("/{userId}")
    public void unfriend(@PathVariable Long userId, Authentication auth) {
        friendService.unfriend(userId, (Long) auth.getPrincipal());
    }
}
