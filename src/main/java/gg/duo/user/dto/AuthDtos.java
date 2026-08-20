package gg.duo.user.dto;

import gg.duo.common.dto.UserDto;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class SignupForm {
        private String email;
        private String password;
        private String name;        // 회원가입 및 아이디 찾기용
        private String phone;       // 회원가입 및 아이디 찾기용
        private String nickname;
        private String gender;
        private String ageRange;
        private String game;
        private String playStyle;
        private String position;
        private boolean mic;
        private String tier;
        private String playTimes; // 콤마 구분
        private String gameModes; // 콤마 구분
        private String riotNickname;
    }

    public record LoginRequest(String email, String password) {}

    public record AuthResponse(String token, UserDto user) {}

    public record ProfileUpdateRequest(
            String nickname, String gender, String ageRange, String game,
            String playStyle, String position, boolean mic, String tier,
            String playTimes, String gameModes, String riotNickname) {}
}