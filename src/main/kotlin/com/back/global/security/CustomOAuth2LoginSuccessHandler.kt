package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

@Component
class CustomOAuth2LoginSuccessHandler(
    private val rq: Rq,
    private val memberService: MemberService,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val actor = rq.actorFromDb ?: throw IllegalStateException("로그인된 사용자가 없습니다.")

        // Access Token과 Refresh Token(apiKey) 생성
        val accessToken = memberService.genAccessToken(actor)
        val refreshToken = actor.apiKey

        // Rq의 헬퍼 메서드를 사용하여 쿠키 설정
        rq.setCrossDomainCookie("accessToken", accessToken, TimeUnit.MINUTES.toSeconds(20).toInt())
        rq.setCrossDomainCookie("apiKey", refreshToken, TimeUnit.DAYS.toSeconds(7).toInt())

        // state 값에서 프론트엔드 리다이렉트 주소 복원
        var redirectUrl = "https://news-ox.vercel.app/"
        val state = request.getParameter("state")
        if (!state.isNullOrBlank()) {
            redirectUrl = runCatching {
                val decoded = Base64.getUrlDecoder().decode(state)
                String(decoded, StandardCharsets.UTF_8)
            }.getOrElse {
                // 디코딩 실패 시 기본 URL 사용
                redirectUrl
            }
        }

        // 토큰 정보가 담기지 않은 URL로 프론트엔드에 리다이렉트
        rq.sendRedirect(redirectUrl)
    }
}
