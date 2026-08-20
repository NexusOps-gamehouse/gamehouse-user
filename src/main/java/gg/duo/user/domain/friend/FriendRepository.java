package gg.duo.user.domain.friend;

import gg.duo.user.domain.friend.Friend;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    /** 두 유저 사이의 관계 (방향 무관, PENDING/ACCEPTED 모두) */
    @Query("""
            select f from Friend f
            where (f.requester.id = :a and f.receiver.id = :b)
               or (f.requester.id = :b and f.receiver.id = :a)
            """)
    Optional<Friend> findBetween(@Param("a") Long a, @Param("b") Long b);

    /**
     * 받은 친구 신청 (대기 중).
     * FriendDto 가 상대방 User 를 통째로 담으므로 두 연관을 같이 가져온다.
     * 안 그러면 신청 1건당 users 조회가 한 번씩 더 나간다(N+1).
     */
    @EntityGraph(attributePaths = {"requester", "receiver"})
    List<Friend> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, Friend.Status status);

    /** 보낸 친구 신청 (대기 중) */
    @EntityGraph(attributePaths = {"requester", "receiver"})
    List<Friend> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, Friend.Status status);

    /** 내 친구 목록 (수락된 관계, 방향 무관) */
    @Query("""
            select f from Friend f
            join fetch f.requester
            join fetch f.receiver
            where f.status = :status
              and (f.requester.id = :userId or f.receiver.id = :userId)
            order by f.acceptedAt desc
            """)
    List<Friend> findAllByStatusInvolving(@Param("userId") Long userId,
                                          @Param("status") Friend.Status status);

    default List<Friend> findFriendsOf(Long userId) {
        return findAllByStatusInvolving(userId, Friend.Status.ACCEPTED);
    }
}
