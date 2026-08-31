package gg.duo.user.controller;

import gg.duo.user.dto.AuthDtos.AuthResponse;
import gg.duo.user.dto.AuthDtos.LoginRequest;
import gg.duo.user.dto.AuthDtos.SignupForm;
import gg.duo.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import gg.duo.user.service.ProfileImageStorageService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfileImageStorageService profileImageStorageService;
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuthResponse signup(@ModelAttribute SignupForm form,
                               @RequestParam(value = "image", required = false) MultipartFile image) {
        return authService.signup(form, image);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 이메일 중복 확인 */
    @GetMapping("/check-email")
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        return Map.of("available", authService.emailAvailable(email));
    }

    /** 닉네임 중복 확인 */
    @GetMapping("/check-nickname")
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        return Map.of("available", authService.nicknameAvailable(nickname));
    }

    @PostMapping("/profile-image/presigned-url")
    public ProfileImageStorageService.PresignedUpload createProfileImageUpload(
            @RequestBody ProfileImageUploadRequest request
    ) {
        return profileImageStorageService.createPresignedUpload(request.contentType());
    }

    public record ProfileImageUploadRequest(String contentType) {}
}