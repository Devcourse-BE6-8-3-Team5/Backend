package com.back.global.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class CustomOAuth2AuthorizationRequestResolver(
    private val clientRegistrationRepository: ClientRegistrationRepository
) : OAuth2AuthorizationRequestResolver {

    // Spring Security 기본 Authorization URI 사용
    private val delegate = DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
    )

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val req = delegate.resolve(request) ?: return null
        return customizeRequest(req, request)
    }

    override fun resolve(request: HttpServletRequest, clientRegistrationId: String?): OAuth2AuthorizationRequest? {
        val req = delegate.resolve(request, clientRegistrationId) ?: return null
        return customizeRequest(req, request)
    }

    private fun customizeRequest(req: OAuth2AuthorizationRequest, request: HttpServletRequest): OAuth2AuthorizationRequest {
        // 개발 환경에서 redirect_uri를 프론트엔드 주소로 변경
        val authorizationRequest = if (request.serverName == "localhost") {
            val newRedirectUri = req.redirectUri?.replace("localhost:8080", "localhost:3000")
            OAuth2AuthorizationRequest.from(req)
                .redirectUri(newRedirectUri)
                .build()
        } else {
            req
        }

        // 요청 파라미터에서 redirectUrl 가져오기, 없으면 "/"
        val redirectUrl = request.getParameter("redirectUrl").orEmpty().ifBlank { "/" }

        // CSRF 방지용 nonce 추가
        val originState = UUID.randomUUID().toString()

        // redirectUrl#originState 결합
        val rawState = "$redirectUrl#$originState"

        // Base64 URL-safe 인코딩
        val encodedState = Base64.getUrlEncoder().encodeToString(rawState.toByteArray(StandardCharsets.UTF_8))

        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .state(encodedState) // state 교체
            .build()
    }
}
