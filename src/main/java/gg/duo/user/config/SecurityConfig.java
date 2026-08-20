package gg.duo.user.config;

import gg.duo.common.security.SecurityBaseConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends SecurityBaseConfig {

    @Override
    protected void configurePublicEndpoints(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(
                "/api/auth/**",
                "/api/users/find-email",
                "/api/users/reset-password",
                "/uploads/**",
                // 서비스 간 호출. Ingress 에 /internal 규칙이 없어 클러스터 밖에서는 닿지 않는다.
                "/internal/**"
        ).permitAll();
    }

    /**
     * 비밀번호 해시는 user 서비스만 다룬다.
     *
     * common 에 두지 않는 이유: 다른 서비스가 이 빈을 주입받을 수 있으면
     * "여기서도 비밀번호를 검증할 수 있겠네"라는 유혹이 생긴다. 인증은 한 곳에서만.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
