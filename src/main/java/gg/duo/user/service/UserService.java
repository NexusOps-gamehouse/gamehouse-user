package gg.duo.user.service;

import gg.duo.common.dto.UserDto;
import gg.duo.user.client.RiotClient;
import gg.duo.user.domain.mastery.UserChampionMastery;
import gg.duo.user.domain.mastery.UserChampionMasteryRepository;
import gg.duo.user.domain.user.User;
import gg.duo.user.domain.user.UserRepository;
import gg.duo.user.dto.AuthDtos.ProfileUpdateRequest;
import gg.duo.user.dto.ChampionMasteryView;
import gg.duo.user.dto.RiotProfileView;
import gg.duo.user.dto.UserMapper;
import gg.duo.user.event.publisher.UserProfileUpdatedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * 라이엇 재동기화 최소 간격.
     *
     * 개발용 키 한도가 '2분당 100회'다. 이 창 하나에 사용자당 1회로 묶어두면
     * 연타를 해도 예산이 무너지지 않는다. 프론트도 같은 값으로 버튼을 잠그지만,
     * 진짜 방어선은 이쪽이다. (프론트는 우회할 수 있다)
     *
     * riot 서비스에도 RateLimiter 가 있지만 성격이 다르다. 저쪽은 "우리 전체가
     * 라이엇에 보내는 총량"을 막고, 이쪽은 "한 사용자가 만드는 호출"을 막는다.
     */
    private static final Duration RIOT_SYNC_COOLDOWN = Duration.ofMinutes(2);

    private final UserRepository userRepository;
    private final UserChampionMasteryRepository userChampionMasteryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RiotClient riotClient;
    private final UserProfileUpdatedPublisher profileUpdatedPublisher;

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return UserMapper.from(userRepository.findById(userId).orElseThrow());
    }

    /** 타 유저 프로필 조회 */
    @Transactional(readOnly = true)
    public UserDto get(Long userId) {
        return UserMapper.from(userRepository.findById(userId).orElseThrow());
    }

    /**
     * 서비스 간 조회용 — post·chat 이 UserClient 로 부르는 자리.
     *
     * 목록 화면 하나가 글 20개를 그리면 작성자 조회가 20번 나간다. 그래서
     * 단건이 아니라 id 묶음으로 받는다. (N+1 은 서비스가 나뉘면 N+1 번의
     * HTTP 왕복이 되어 훨씬 비싸진다)
     */
    @Transactional(readOnly = true)
    public List<UserDto> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return userRepository.findAllById(ids).stream().map(UserMapper::from).toList();
    }

    /** 서비스 간 조회용 — 닉네임 검색. post 의 "작성자 닉네임으로 검색"이 쓴다. */
    @Transactional(readOnly = true)
    public List<Long> findIdsByNicknameContaining(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return userRepository.findIdsByNicknameContaining(keyword.trim());
    }

    @Transactional
    public UserDto updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        if (req.nickname() != null && !req.nickname().isBlank()
                && !req.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(req.nickname()))
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            user.setNickname(req.nickname());
        }
        user.setGender(req.gender());
        user.setAgeRange(req.ageRange());
        user.setGame(req.game());
        user.setPlayStyle(req.playStyle());
        user.setPosition(req.position());
        user.setMic(req.mic());
        user.setTier(req.tier());
        user.setPlayTimes(req.playTimes());
        user.setGameModes(req.gameModes());
        user.setRiotNickname(req.riotNickname());

        // 닉네임·프로필 이미지를 복제해 둔 서비스(chat 메시지 발신자 스냅샷)에 알린다.
        profileUpdatedPublisher.publish(user);

        return UserMapper.from(user);
    }

    /** [이메일 찾기] 이름 + 전화번호 기준 */
    @Transactional(readOnly = true)
    public String findEmailByNameAndPhone(String name, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("이름을 입력해주세요.");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("전화번호를 입력해주세요.");

        // 저장 쪽(AuthService.normalizePhone)과 같은 규칙으로 맞춘다.
        // "-" 만 지우면 "010 1234 5678" 처럼 공백이 섞인 입력은 여전히 못 찾는다.
        String cleanName = name.trim();
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        // 정규화 이전에 저장된 계정(하이픈 포함)도 있을 수 있어 원본 → 정규화 순으로 두 번 찾는다.
        User user = userRepository.findByNameAndPhone(cleanName, phone)
                .orElseGet(() -> userRepository.findByNameAndPhone(cleanName, cleanPhone)
                        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.")));

        return maskEmail(user.getEmail());
    }

    /** [비밀번호 재설정] 이메일 + 이름 + 전화번호로 본인 확인 후 새 비밀번호로 변경 */
    @Transactional
    public void resetPassword(String email, String name, String phone,
                              String newPassword, String newPasswordConfirm) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("이름을 입력해주세요.");
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        if (newPassword == null || newPassword.length() < 4)
            throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
        if (!newPassword.equals(newPasswordConfirm))
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");

        // findEmailByNameAndPhone 과 동일한 정규화 규칙
        String cleanName = name.trim();
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        User user = userRepository.findByEmailAndNameAndPhone(email, cleanName, phone)
                .orElseGet(() -> userRepository.findByEmailAndNameAndPhone(email, cleanName, cleanPhone)
                        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.")));

        if (passwordEncoder.matches(newPassword, user.getPassword()))
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요.");

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * [라이엇 프로필 동기화]
     *
     * 라이엇 API 를 직접 부르지 않고 riot 서비스에 물어본다. 키·레이트리밋·
     * 외부 네트워크(NAT Gateway 경유)를 한 서비스에 몰아두면, 라이엇이 느려지거나
     * 429 를 뱉어도 그 영향이 user 파드의 스레드 풀까지 번지지 않는다.
     *
     * 여기서 저장하는 것은 계정 식별 값과 스냅샷이다.
     *   - puuid : 이후 조회의 기준 키
     *   - gameName / tagLine : 라이엇이 돌려준 정규화된 값(대소문자 등)
     * 티어(user.tier)는 사용자가 프로필에서 직접 고르는 한글 값("다이아몬드")이므로
     * 라이엇의 영문 값("DIAMOND")으로 덮어쓰지 않는다.
     */
    @Transactional
    public RiotProfileView syncRiotProfile(Long userId, String gameName, String tagLine) {
        User user = userRepository.findById(userId).orElseThrow();

        boolean sameAccount = isSameRiotId(user, gameName, tagLine);

        /*
         * [쿨다운] 최근에 갱신했고 같은 계정이면 riot 을 부르지 않고 저장된 값을 돌려준다.
         *
         * 이게 없으면 사용자가 "다시 불러오기"를 연타하는 것만으로 한도에 닿는다.
         * 실제로 35번 클릭에 137회 호출이 나가 429 를 맞았다.
         * (한 번 누를 때마다 라이엇 API 를 3~4회 부르기 때문이다.)
         *
         * 계정이 바뀌면(다른 Riot ID 입력) 쿨다운을 적용하지 않는다.
         * 그건 '갱신'이 아니라 '연동 대상 변경'이라 기다리게 할 이유가 없다.
         */
        if (sameAccount
                && user.getRiotSyncedAt() != null
                && user.getRiotSyncedAt().isAfter(Instant.now().minus(RIOT_SYNC_COOLDOWN))) {
            return storedRiotProfile(userId);
        }

        /*
         * [puuid 재사용] 같은 계정이면 Account API 호출을 건너뛴다.
         * puuid 는 계정에 영구적으로 붙는 값이라 다시 물어볼 이유가 없다.
         */
        RiotProfileView profile = (sameAccount && user.getPuuid() != null)
                ? riotClient.fetchProfileByPuuid(user.getPuuid(), user.getGameName(), user.getTagLine())
                : riotClient.fetchProfile(gameName, tagLine);

        user.setPuuid(profile.puuid());
        user.setGameName(profile.gameName());
        user.setTagLine(profile.tagLine());

        // 예전에는 식별자 세 개만 저장하고 나머지는 응답으로만 흘려보냈다.
        // 그래서 마이페이지를 떠나는 순간 화면의 레벨·티어·LP·승패가 전부 사라졌다.
        user.setProfileIconId(profile.profileIconId());
        user.setSummonerLevel(profile.summonerLevel());
        user.setRiotTier(profile.tier());
        user.setRiotRank(profile.rank());
        user.setLeaguePoints(profile.leaguePoints());
        user.setWins(profile.wins());
        user.setLosses(profile.losses());

        Instant syncedAt = Instant.now();
        user.setRiotSyncedAt(syncedAt);

        saveMasteries(user, profile.championMasteries());

        // 프론트가 쿨다운 남은 시간을 계산할 수 있도록 갱신 시각을 실어 보낸다.
        return profile.withSyncedAt(syncedAt);
    }

    /**
     * 요청한 Riot ID 가 이미 연동된 계정과 같은가.
     *
     * 대소문자와 앞뒤 공백은 무시한다. 라이엇이 돌려주는 정규화된 표기와
     * 사용자가 입력한 표기가 다를 수 있어서다. (예: "hide on bush" vs "Hide on bush")
     */
    private boolean isSameRiotId(User user, String gameName, String tagLine) {
        return user.getGameName() != null
                && user.getTagLine() != null
                && gameName != null
                && tagLine != null
                && user.getGameName().equalsIgnoreCase(gameName.trim())
                && user.getTagLine().equalsIgnoreCase(tagLine.trim());
    }

    /**
     * 모스트 챔피언을 통째로 갈아끼운다.
     *
     * 병합하지 않고 지웠다 다시 넣는 이유는, 숙련도 순위가 바뀌면 1~3위 구성 자체가
     * 달라지기 때문이다. 예전 1위가 이번엔 목록 밖으로 밀려날 수 있는데,
     * 갱신만 하면 그 행이 남아 유령 데이터가 된다.
     *
     * delete 가 insert 보다 먼저 나가도록 flush 를 한 번 끼운다.
     */
    private void saveMasteries(User user, List<ChampionMasteryView> masteries) {
        userChampionMasteryRepository.deleteByUser(user);
        userChampionMasteryRepository.flush();

        if (masteries == null || masteries.isEmpty()) return;

        Instant now = Instant.now();
        List<UserChampionMastery> rows = masteries.stream()
                .map(m -> UserChampionMastery.builder()
                        .user(user)
                        .game("LOL")
                        .ranking(m.ranking())
                        .championId(m.championId())
                        .masteryLevel(m.championMasteryLevel())
                        .masteryPoints(m.championMasteryPoints())
                        .syncedAt(now)
                        .build())
                .toList();

        userChampionMasteryRepository.saveAll(rows);
    }

    /**
     * 저장해 둔 라이엇 프로필을 그대로 돌려준다. riot 서비스를 부르지 않는다.
     *
     * 마이페이지에 들어올 때마다 호출되므로 외부를 부르면 안 된다.
     * 개발용 키는 24시간마다 만료되고 레이트 리밋도 2분 100회라,
     * 페이지를 몇 번 오가는 것만으로 한도에 닿는다.
     * 갱신은 사용자가 "다시 불러오기"를 눌렀을 때만(syncRiotProfile) 일어난다.
     *
     * 아직 연동하지 않았으면 null.
     */
    @Transactional(readOnly = true)
    public RiotProfileView storedRiotProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getGameName() == null) return null;

        List<ChampionMasteryView> masteries =
                userChampionMasteryRepository.findByUserOrderByRankingAsc(user).stream()
                        .map(m -> new ChampionMasteryView(m.getRanking(), m.getChampionId(),
                                m.getMasteryLevel(), m.getMasteryPoints()))
                        .toList();

        return new RiotProfileView(
                user.getPuuid(),
                user.getGameName(),
                user.getTagLine(),
                user.getProfileIconId(),
                user.getSummonerLevel(),
                user.getRiotTier(),
                user.getRiotRank(),
                user.getLeaguePoints(),
                user.getWins(),
                user.getLosses(),
                masteries,
                user.getRiotSyncedAt());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 2) {
            return id.charAt(0) + "*@" + domain;
        } else {
            return id.substring(0, 2) + "*".repeat(id.length() - 2) + "@" + domain;
        }
    }
}
