package com.bbangpatrol.common.config;

import com.bbangpatrol.common.filter.JwtAuthenticationEntryPoint;
import com.bbangpatrol.common.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health", // 배포 정상 동작 확인 주소
                                "/api/v1/auth/login", // 로그인 주소
                                "/api/v1/auth/reissue",// 재발급 주소
                                "/error"
                        ).permitAll()
                        // 비로그인도 볼 수 있는 조회 API
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/stores/search",          // 지도 검색
                                "/api/v1/stores/*/detail",        // 가게 상세
                                "/api/v1/stores/*/attractions",   // 주변 관광지
                                "/api/v1/stores/*/reviews"        // 리뷰 목록
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
