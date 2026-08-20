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
        user.setProfileImageUrl(fileStorageService.store(image));
        user.setGender(form.getGender());
        user.setAgeRange(form.getAgeRange());
        user.setGame(form.getGame());
        user.setPlayStyle(form.getPlayStyle());
        user.setPosition(form.getPosition());
        user.setMic(form.isMic());
        user.setTier(form.getTier());
        user.setPlayTimes(form.getPlayTimes());
        user.setGameModes(form.getGameModes());
        user.setRiotNickname(form.getRiotNickname());
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        return new AuthResponse(jwtTokenProvider.createToken(user.getId()), UserMapper.from(user));
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