package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import com.back.global.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val memberService: MemberService,
    private val rq: Rq,
) : OncePerRequestFilter() {

    private val publicPaths = setOf(
        "/swagger-ui/",
        "/v3/api-docs/",
        "/swagger-resources/",
        "/h2-console"
    )

    private val publicExactPaths = setOf(
        "/api/news",
        "/api/members/rank",
        "/api/quiz/fact",
        "/api/quiz/fact/category",
        "/api/members/login",
        "/api/members/join"
    )

    private val authRequiredPaths = setOf(
        "/api/members/info",
        "/api/members/logout",
        "/api/members/withdraw"
    )

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        try {
            work(request, response, filterChain)
        } catch (e: ServiceException) {
            val rsData: RsData<Void?> = e.rsData
            response.contentType = "$APPLICATION_JSON_VALUE; charset=UTF-8"
            response.status = rsData.code
            val jsonResponse = Ut.json.toString(rsData)
                ?: """{"resultCode":"${rsData.code}","msg":"${rsData.message}"}"""
            response.writer.write(jsonResponse)
        }
    }

    private fun work(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val uri = request.requestURI
        val method = request.method

        // permitAll 체크
        if (isPublicRequest(uri, method)) {
            filterChain.doFilter(request, response)
            return
        }
        // 인증이 필요한 URL 체크
        if (!isAuthRequired(uri, method)) {
            filterChain.doFilter(request, response)
            return
        }

        val (apiKey, accessToken) = extractTokens()
        if (apiKey.isBlank() && accessToken.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        val (member, isAccessTokenValid) = resolveMember(apiKey, accessToken)

        if (accessToken.isNotBlank() && !isAccessTokenValid) {
            refreshAccessToken(member)
        }

        authenticate(member)

        filterChain.doFilter(request, response)
    }

    private fun isPublicRequest(uri: String, method: String): Boolean {
        if (publicPaths.any { uri.startsWith(it) }) return true
        if (uri in publicExactPaths) return true
        if (method == "GET" && uri.startsWith("/api/news/")) return true
        return false
    }

    private fun isAuthRequired(uri: String, method: String): Boolean {
        return uri.startsWith("/api/quiz/detail/") ||
                uri.startsWith("/api/quiz/daily/") ||
                uri.startsWith("/api/admin/") ||
                uri in authRequiredPaths ||
                (method == "GET" && uri.matches(Regex("/api/quiz/fact/\\d+"))) ||
                (method == "POST" && uri.matches(Regex("/api/quiz/fact/submit/\\d+")))
    }

    private fun extractTokens(): Pair<String, String> {
        val headerAuthorization = rq.getHeader("Authorization", "")

        return if (headerAuthorization.isNotBlank()) {
            if (!headerAuthorization.startsWith("Bearer "))
                throw ServiceException(401, "Authorization 헤더가 Bearer 형식이 아닙니다.")
            val bits = headerAuthorization.split(" ", limit = 3)
            bits.getOrNull(1).orEmpty() to bits.getOrNull(2).orEmpty()
        } else {
            rq.getCookieValue("apiKey", "") to rq.getCookieValue("accessToken", "")
        }
    }

    private fun resolveMember(apiKey: String, accessToken: String): Pair<Member, Boolean> {
        memberFromAccessToken(accessToken)?.let { return it to true }

        val member = memberService.findByApiKey(apiKey).orElse(null)
            ?: throw ServiceException(401, "API 키가 유효하지 않습니다.")

        return member to false
    }

    private fun memberFromAccessToken(token: String): Member? {
        if (token.isBlank()) return null
        val payload = memberService.payload(token) ?: return null

        val id = payload["id"] as Long
        val email = payload["email"] as String
        val name = payload["name"] as String
        val role = payload["role"] as String

        return Member(id, email, name, role)
    }

    private fun refreshAccessToken(member: Member) {
        val newToken = memberService.genAccessToken(member)
        rq.setCrossDomainCookie("accessToken", newToken, 60 * 20)
        rq.setHeader("Authorization", newToken)
    }

    private fun authenticate(member: Member) {
        val user: UserDetails = SecurityUser(
            member.id,
            member.email,
            member.name,
            "",
            member.authorities
        )

        val authentication: Authentication =
            UsernamePasswordAuthenticationToken(user, user.password, user.authorities)

        SecurityContextHolder.getContext().authentication = authentication
    }
}
