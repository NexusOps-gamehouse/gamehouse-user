package gg.duo.user.service;

import gg.duo.user.dto.UserMapper;
import gg.duo.user.dto.AuthDtos.AuthResponse;
import gg.duo.user.dto.AuthDtos.LoginRequest;
import gg.duo.user.dto.AuthDtos.SignupForm;
import gg.duo.common.dto.UserDto;
import gg.duo.user.domain.user.User;
import gg.duo.user.domain.user.UserRepository;
import gg.duo.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;
    private final SurveyService surveyService;

    @Transactional
    public AuthResponse signup(SignupForm form) {
        return signup(form, null);
    }

    @Transactional
    public AuthResponse signup(SignupForm form, MultipartFile image) {
        if (form.getEmail() == null || form.getEmail().isBlank())
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        if (form.getPassword() == null || form.getPassword().length() < 4)
            throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
        if (form.getNickname() == null || form.getNickname().isBlank())
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        // name / phone 은 아이디 찾기·비밀번호 재설정의 유일한 열쇠다.
        // 비어 있으면 그 계정은 본인 확인 수단이 영영 없으므로 이메일·닉네임과 같은 급으로 막는다.
        if (form.getName() == null || form.getName().isBlank())
            throw new IllegalArgumentException("이름을 입력해주세요.");
        if (form.getPhone() == null || form.getPhone().isBlank())
            throw new IllegalArgumentException("전화번호를 입력해주세요.");
        if (userRepository.existsByEmail(form.getEmail()))
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        if (userRepository.existsByNickname(form.getNickname()))
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");

        User user = new User();
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setNickname(form.getNickname());
        // 이 두 줄이 빠져 있어서 모든 회원의 name / phone 이 NULL 이었고,
        // 아이디 찾기가 어떤 값을 넣어도 "일치하는 회원 정보를 찾을 수 없습니다"를 냈다.
        // 폼도 DTO 도 엔티티도 조회 로직도 멀쩡했고, 저장하는 곳만 없었다.
        user.setName(form.getName().trim());
        user.setPhone(normalizePhone(form.getPhone()));
        if (form.getProfileImageKey() != null && !form.getProfileImageKey().isBlank()) {
            String key = form.getProfileImageKey();

            if (!key.matches("^profile-images/[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp)$")) {
                throw new IllegalArgumentException("올바르지 않은 프로필 이미지 경로입니다.");
            }

            user.setProfileImageUrl(key);
        } else {
            user.setProfileImageUrl(fileStorageService.store(image));
        }
        // [FR-01] 프로필 정보 5개
        user.setAge(form.getAge());
        user.setMic(form.isMic());
        user.setPlayTimes(form.getPlayTimes());
        user.setPlayDays(form.getPlayDays());
        user.setPlayDuration(form.getPlayDuration());
        user.setRiotNickname(form.getRiotNickname());
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        /*
         * 성향 설문을 가입과 같은 트랜잭션에서 저장한다.
         *
         * 별도 API 로 나누면 "계정은 생겼는데 설문만 실패한" 사용자가 남는다.
         * 그 계정은 매칭 점수를 낼 수 없고, 다시 채우게 만들 화면도 아직 없다.
         * 여기서 예외가 나면 계정 생성까지 함께 롤백되는 편이 낫다.
         *
         * 설문을 건너뛴 가입(선택 단계로 돌릴 경우)도 있을 수 있어 값이 없으면 조용히 넘어간다.
         */
        if (form.getSurveyAnswers() != null && !form.getSurveyAnswers().isBlank())
            surveyService.submit(user.getId(), parseAnswers(form.getSurveyAnswers()));

        return new AuthResponse(jwtTokenProvider.createToken(user.getId()), UserMapper.from(user));
    }

    /**
     * "3,5,1,4,..." → [3, 5, 1, 4, ...]
     *
     * multipart 폼이라 배열을 그대로 받기 어려워 콤마 문자열로 온다.
     * 값 검증(개수·1~5 범위)은 SurveyService 가 한다. 여기서는 형태만 바꾼다.
     */
    private java.util.List<Integer> parseAnswers(String csv) {
        try {
            return java.util.Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("설문 응답 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 전화번호를 숫자만 남긴 형태로 통일한다.
     *
     * 프론트가 자동 하이픈을 넣어주지만(010-1234-5678), API 를 직접 호출하거나
     * 그 기능이 없던 시절에 가입한 경우 형식이 섞인다. 저장 시점에 하나로 맞춰두면
     * 아이디 찾기에서 형식 차이 때문에 못 찾는 일이 없다.
     *
     * 조회 쪽(UserService)도 같은 규칙으로 정규화하므로 양쪽이 항상 같은 형태로 만난다.
     */
    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }

    @Transactional(readOnly = true)
    public boolean emailAvailable(String email) {
        return email != null && !email.isBlank() && !userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean nicknameAvailable(String nickname) {
        return nickname != null && !nickname.isBlank() && !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        user.setLastActiveAt(Instant.now());
        return new AuthResponse(jwtTokenProvider.createToken(user.getId()), UserMapper.from(user));
    }
}