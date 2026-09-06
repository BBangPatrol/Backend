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
                        // 로그인 상태면 필터가 인증을 채워주므로 userId 로 개인화된 응답(즐겨찾기 여부 등)이 나간다
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/stores/search",          // 지도 검색
                                "/api/v1/stores/*/detail",        // 가게 상세
                                "/api/v1/stores/*/attractions",   // 주변 관광지
                                "/api/v1/stores/*/reviews"        // 리뷰 목록
                        ).permitAll()
                        // 그 외(즐겨찾기, 리뷰 작성/수정/삭제/좋아요, 방문 인증, 미션, 수집품, 마이페이지)는 로그인 필요
                        .anyRequest().authenticated()
                )
                // 인증 실패 시 403 대신 명세대로 401 + ApiResponse 포맷으로 응답
                .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // @AuthenticationPrincipal Long userId를 사용하기 위한 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}