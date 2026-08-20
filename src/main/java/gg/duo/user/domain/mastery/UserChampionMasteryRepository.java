package gg.duo.user.domain.mastery;

import gg.duo.user.domain.user.User;
import gg.duo.user.domain.mastery.UserChampionMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserChampionMasteryRepository extends JpaRepository<UserChampionMastery, Long> {

    List<UserChampionMastery> findByUserOrderByRankingAsc(User user);

    void deleteByUser(User user);
}
