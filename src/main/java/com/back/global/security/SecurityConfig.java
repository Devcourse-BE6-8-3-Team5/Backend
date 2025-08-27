package com.back.global.security;

import com.back.global.rsData.RsData;
import com.back.global.standard.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Lazy
    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final AuthenticationSuccessHandler customOAuth2LoginSuccessHandler;
    private final CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/favicon.ico").permitAll()
                                .requestMatchers("/h2-console/**").permitAll()
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

                                //모두 접근 가능한 API
                                .requestMatchers(HttpMethod.GET, "/api/news", "/api/news/*/**", "/api/quiz/fact","/api/quiz/fact/category").permitAll() // 모든 뉴스 조회, fact퀴즈 다건조회/카테고리별 조회는 모두 허용
                                .requestMatchers(HttpMethod.POST, "/api/members/login", "/api/members/join").permitAll() // 로그인, 회원가입은 모두 허용
                                .requestMatchers(HttpMethod.GET, "/api/members/rank").permitAll() // 랭킹 조회는 모두 허용

                                // 회원만 접근 가능한 API
                                .requestMatchers("/api/quiz/detail/*/**").authenticated() // 상세퀴즈에 대한 모든 HTTP 메서드 요청은 로그인한 사용자만 허용
                                .requestMatchers("/api/quiz/daily/*/**").authenticated() // 오늘의퀴즈에 대한 모든 HTTP 메서드 요청은 로그인한 사용자만 허용
                                .requestMatchers(HttpMethod.GET, "/api/quiz/fact/{id}").authenticated() // fact퀴즈 단건 조회 GET 요청은 로그인한 사용자만 허용
                                .requestMatchers(HttpMethod.POST, "/api/quiz/fact/submit/{id}").authenticated() // fact퀴즈 제출 POST 요청은 로그인한 사용자만 허용
                                .requestMatchers( "/api/members/info").authenticated() // 마이페이지 조회, 수정
                                .requestMatchers(HttpMethod.DELETE, "/api/members/withdraw", "/api/members/logout").authenticated() // 회원탈퇴, 로그아웃

                                // 관리자만 접근 가능한 API
                                .requestMatchers("/api/admin/*/**").hasRole("ADMIN") // 관리자 페이지의 모든 HTTP 메서드 요청은 ADMIN 권한이여야함

                                // 그 외는 모두 인증 필요
                                .requestMatchers("/api/*/**").authenticated()

                                // 그 외는 모두 허용
                                .anyRequest().permitAll()
                )
                .headers(
                        headers -> headers
                                .frameOptions(
                                        HeadersConfigurer.FrameOptionsConfig::sameOrigin
                                )
                ).csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(STATELESS))
                .oauth2Login(oauth2Login -> oauth2Login
                        .successHandler(customOAuth2LoginSuccessHandler)
                        .authorizationEndpoint(
                                authorizationEndpoint -> authorizationEndpoint
                                        .authorizationRequestResolver(customOAuth2AuthorizationRequestResolver)
                        )
                )
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(401);
                                            response.getWriter().write(
                                                    Ut.json.toString(
                                                            new RsData<Void>(
                                                                    401,
                                                                    "로그인 후 이용해주세요.",
                                                                    null
                                                            )
                                                    )
                                            );
                                        }
                                )
                                .accessDeniedHandler(
                                        (request, response, accessDeniedException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(403);
                                            response.getWriter().write(
                                                    Ut.json.toString(
                                                            new RsData<Void>(
                                                                    403,
                                                                    "권한이 없습니다.",
                                                                    null
                                                            )
                                                    )
                                            );
                                        }
                                )
                );
        return http.build();
    }


    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 설정
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://news-ox.vercel.app"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));

        // 자격 증명 허용 설정
        configuration.setAllowCredentials(true); // 쿠키 허용

        // 허용할 헤더 설정
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용

        // CORS 설정을 소스에 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/admin/**", configuration);
        return source;
    }
}
