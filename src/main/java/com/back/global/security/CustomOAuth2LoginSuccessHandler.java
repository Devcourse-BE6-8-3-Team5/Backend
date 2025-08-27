package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rq.Rq;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final Rq rq;
    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Member actor = rq.getActorFromDb();

        // Access Token과 Refresh Token(apiKey) 생성
        String accessToken = memberService.genAccessToken(actor);
        String refreshToken = actor.getApiKey();

        // Rq의 헬퍼 메서드를 사용하여 쿠키 설정
        rq.setCrossDomainCookie("accessToken", accessToken, (int) TimeUnit.MINUTES.toSeconds(20));
        rq.setCrossDomainCookie("refreshToken", refreshToken, (int) TimeUnit.DAYS.toSeconds(7));

        // state 값에서 프론트엔드 리다이렉트 주소 복원
        String redirectUrl = "https://news-ox.vercel.app/";
        String state = request.getParameter("state");
        if (state != null && !state.isBlank()) {
            try {
                String decodedUrl = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
                redirectUrl = decodedUrl;
            } catch (IllegalArgumentException e) {
                // 디코딩 실패 시 기본 URL 사용
            }
        }

        // 토큰 정보가 담기지 않은 URL로 프론트엔드에 리다이렉트
        rq.sendRedirect(redirectUrl);
    }
}