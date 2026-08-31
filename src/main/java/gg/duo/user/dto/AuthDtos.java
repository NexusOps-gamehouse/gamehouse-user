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
        private String profileImageKey;

        // [FR-01] 가입 설문에서 받는 프로필 정보 5개.
        //   성별·주 포지션·게임·게임모드·게임성향·티어는 가입에서 빠졌다.
        //   (포지션과 게임 모드는 파티마다 달라지므로 매칭/파티 생성 화면이 받는다)
        private boolean mic;
        private Integer age;
        private String playTimes;    // 콤마 구분
        private String playDays;     // 콤마 구분
        private String playDuration; // "2~4시간"

        /** 플레이 성향 설문 12문항, 콤마 구분. ("3,5,1,4,...") 미응답이면 null. */
        private String surveyAnswers;

        private String riotNickname;
    }

    public record LoginRequest(String email, String password) {}

    public record AuthResponse(String token, UserDto user) {}

    public record ProfileUpdateRequest(
            String nickname, Integer age, String game,
            String playStyle, String position, boolean mic, String tier,
            String playTimes, String playDays, String playDuration,
            String gameModes, String riotNickname) {}
}