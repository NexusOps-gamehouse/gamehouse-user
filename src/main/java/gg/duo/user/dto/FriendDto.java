package gg.duo.user.dto;

import gg.duo.common.dto.UserDto;
import gg.duo.user.domain.friend.Friend;
import gg.duo.user.domain.user.User;

import java.time.Instant;

/**
 * 친구 관계 응답 DTO.
 * user = 나를 기준으로 한 상대방 정보.
 */
public record FriendDto(
        Long id,
        String status,
        Instant createdAt,
        Instant acceptedAt,
        UserDto user
) {
    /** meId 기준으로 상대방을 담아 변환 */
    public static FriendDto from(Friend f, Long meId) {
        User other = f.getRequester().getId().equals(meId) ? f.getReceiver() : f.getRequester();
        return new FriendDto(f.getId(), f.getStatus().name(),
                f.getCreatedAt(), f.getAcceptedAt(), UserMapper.from(other));
    }
}
