package gg.duo.user.service;

import gg.duo.user.dto.FriendDto;
import gg.duo.user.domain.friend.Friend;
import gg.duo.user.domain.user.User;
import gg.duo.user.domain.friend.FriendRepository;
import gg.duo.user.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 친구 신청.
     * 상대가 이미 나에게 신청해둔 상태(역방향 PENDING)면 새로 만들지 않고 그 신청을 수락 처리한다.
     */
    @Transactional
    public FriendDto request(Long receiverId, Long meId) {
        if (receiverId.equals(meId))
            throw new IllegalStateException("자기 자신에게는 친구 신청할 수 없습니다.");

        User receiver = userRepository.findById(receiverId).orElseThrow();
        User me = userRepository.findById(meId).orElseThrow();

        Friend existing = friendRepository.findBetween(meId, receiverId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == Friend.Status.ACCEPTED)
                throw new IllegalStateException("이미 친구입니다.");
            if (existing.getRequester().getId().equals(meId))
                throw new IllegalStateException("이미 친구 신청을 보냈습니다.");
            // 상대가 먼저 보낸 신청이 대기 중 → 수락으로 처리
            return acceptInternal(existing, meId);
        }

        Friend friend = new Friend();
        friend.setRequester(me);
        friend.setReceiver(receiver);
        friendRepository.save(friend);

        notificationService.notify(receiver,
                me.getNickname() + "님이 친구 신청을 보냈습니다.", "/friends");
        return FriendDto.from(friend, meId);
    }

    /** 받은/보낸 대기 중인 신청 목록 */
    @Transactional(readOnly = true)
    public Map<String, List<FriendDto>> pendingRequests(Long meId) {
        List<FriendDto> received = friendRepository
                .findByReceiverIdAndStatusOrderByCreatedAtDesc(meId, Friend.Status.PENDING)
                .stream().map(f -> FriendDto.from(f, meId)).toList();
        List<FriendDto> sent = friendRepository
                .findByRequesterIdAndStatusOrderByCreatedAtDesc(meId, Friend.Status.PENDING)
                .stream().map(f -> FriendDto.from(f, meId)).toList();
        return Map.of("received", received, "sent", sent);
    }

    /** 친구 신청 수락 (받은 사람만 가능) */
    @Transactional
    public FriendDto accept(Long requestId, Long meId) {
        Friend friend = friendRepository.findById(requestId).orElseThrow();
        if (!friend.getReceiver().getId().equals(meId))
            throw new SecurityException("본인이 받은 신청만 수락할 수 있습니다.");
        if (friend.getStatus() != Friend.Status.PENDING)
            throw new IllegalStateException("이미 처리된 신청입니다.");
        return acceptInternal(friend, meId);
    }

    /** 친구 신청 거절(받은 사람) 또는 취소(보낸 사람) */
    @Transactional
    public void deleteRequest(Long requestId, Long meId) {
        Friend friend = friendRepository.findById(requestId).orElseThrow();
        boolean involved = friend.getRequester().getId().equals(meId)
                || friend.getReceiver().getId().equals(meId);
        if (!involved)
            throw new SecurityException("본인이 관련된 신청만 처리할 수 있습니다.");
        if (friend.getStatus() != Friend.Status.PENDING)
            throw new IllegalStateException("대기 중인 신청만 거절/취소할 수 있습니다.");

        friendRepository.delete(friend);
    }

    /** 내 친구 목록 */
    @Transactional(readOnly = true)
    public List<FriendDto> friends(Long meId) {
        return friendRepository.findFriendsOf(meId)
                .stream().map(f -> FriendDto.from(f, meId)).toList();
    }

    /** 친구 삭제 */
    @Transactional
    public void unfriend(Long otherUserId, Long meId) {
        Friend friend = friendRepository.findBetween(meId, otherUserId)
                .filter(f -> f.getStatus() == Friend.Status.ACCEPTED)
                .orElseThrow(() -> new IllegalArgumentException("친구 관계가 아닙니다."));
        friendRepository.delete(friend);
    }

    private FriendDto acceptInternal(Friend friend, Long meId) {
        friend.setStatus(Friend.Status.ACCEPTED);
        friend.setAcceptedAt(Instant.now());

        User accepter = friend.getReceiver();
        notificationService.notify(friend.getRequester(),
                accepter.getNickname() + "님이 친구 신청을 수락했습니다.", "/friends");
        return FriendDto.from(friend, meId);
    }
}
