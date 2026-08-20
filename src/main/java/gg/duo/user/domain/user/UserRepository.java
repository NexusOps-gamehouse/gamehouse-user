package gg.duo.user.domain.user;

import gg.duo.user.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    // [아이디 찾기] 이름과 전화번호로 조회
    Optional<User> findByNameAndPhone(String name, String phone);

    // [비밀번호 재설정] 이메일, 이름, 전화번호로 조회
    Optional<User> findByEmailAndNameAndPhone(String email, String name, String phone);

    /**
     * 닉네임 부분 일치로 id 만 뽑는다. post 의 "작성자 닉네임 검색"이 쓴다.
     *
     * User 엔티티가 아니라 id 만 돌려주는 이유: 호출하는 쪽은 posts 를 걸러낼
     * authorId 목록만 있으면 된다. 엔티티를 통째로 넘기면 서비스 경계를 넘어
     * users 의 모든 컬럼이 흘러간다.
     */
    @Query("select u.id from User u where u.nickname like concat('%', :keyword, '%')")
    List<Long> findIdsByNicknameContaining(@Param("keyword") String keyword);
}
