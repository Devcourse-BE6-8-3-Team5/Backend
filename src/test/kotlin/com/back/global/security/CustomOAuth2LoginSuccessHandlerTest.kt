package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class CustomOAuth2LoginSuccessHandlerTest {

    @Mock
    private lateinit var rq: Rq

    @Mock
    private lateinit var memberService: MemberService

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    private lateinit var handler: CustomOAuth2LoginSuccessHandler

    @BeforeEach
    fun setUp() {
        handler = CustomOAuth2LoginSuccessHandler(rq, memberService)
    }

    @Test
    @DisplayName("로그인 성공 시 AccessToken과 RefreshToken 쿠키가 설정되고 리다이렉트된다.")
    fun testOnAuthenticationSuccess_setsCookiesAndRedirects()
    {
        // GIVEN
        val memberId = 1L
        val memberEmail = "test@example.com"
        val memberName = "TestUser"
        val memberApiKey = "test-api-key"
        val accessToken = "test-access-token"
        val expectedRedirectUrl = "https://news-ox.vercel.app/"

        val securityUser = SecurityUser(
            id = memberId,
            email = memberEmail,
            name = memberName,
            password = "",
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        val authentication = TestingAuthenticationToken(securityUser, null, securityUser.authorities)

        val loggedInMember = Member(
            name = memberName,
            email = memberEmail,
            password = "",
            exp = 0,
            level = 1,
            role = "USER",
            apikey = memberApiKey,
            oauthId = null // oauthId는 null로 설정
        ).apply { this.id = memberId } // id는 apply 블록에서 설정

        `when`(rq.actorFromDb).thenReturn(loggedInMember)
        `when`(memberService.genAccessToken(loggedInMember)).thenReturn(accessToken)
        `when`(request.getParameter("state")).thenReturn(null) // state 없음

        // doAnswer를 사용하여 호출된 인자들을 저장
        val capturedNames = mutableListOf<String>()
        val capturedValues = mutableListOf<String>()
        val capturedExpiries = mutableListOf<Int>()

        Mockito.doAnswer {
            capturedNames.add(it.getArgument(0))
            capturedValues.add(it.getArgument(1))
            capturedExpiries.add(it.getArgument(2))
            null // void 메소드이므로 null 반환
        }.`when`(rq).setCrossDomainCookie(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())

        // WHEN
        handler.onAuthenticationSuccess(request, response, authentication)

        // THEN
        // setCrossDomainCookie가 두 번 호출되었는지 확인
        verify(rq, Mockito.times(2)).setCrossDomainCookie(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())

        // 첫 번째 호출 (accessToken)
        assertThat(capturedNames[0]).isEqualTo("accessToken")
        assertThat(capturedValues[0]).isEqualTo(accessToken)
        assertThat(capturedExpiries[0]).isEqualTo(TimeUnit.MINUTES.toSeconds(20).toInt())

        // 두 번째 호출 (apiKey)
        assertThat(capturedNames[1]).isEqualTo("apiKey")
        assertThat(capturedValues[1]).isEqualTo(memberApiKey)
        assertThat(capturedExpiries[1]).isEqualTo(TimeUnit.DAYS.toSeconds(7).toInt())

        verify(rq).sendRedirect(expectedRedirectUrl)
    }

    @Test
    @DisplayName("state 파라미터가 있으면 해당 URL로 리다이렉트된다.")
    fun testOnAuthenticationSuccess_redirectsWithState() {
        // GIVEN
        val memberId = 1L
        val memberEmail = "test@example.com"
        val memberName = "TestUser"
        val memberApiKey = "test-api-key"
        val accessToken = "test-access-token"
        val stateRedirectUrl = "https://my-frontend.com/auth/callback"
        val encodedState = java.util.Base64.getUrlEncoder().encodeToString(stateRedirectUrl.toByteArray())

        val securityUser = SecurityUser(
            id = memberId,
            email = memberEmail,
            name = memberName,
            password = "",
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        val authentication = TestingAuthenticationToken(securityUser, null, securityUser.authorities)

        val loggedInMember = Member(
            name = memberName,
            email = memberEmail,
            password = "",
            exp = 0,
            level = 1,
            role = "USER",
            apikey = memberApiKey,
            oauthId = null // oauthId는 null로 설정
        ).apply { this.id = memberId } // id는 apply 블록에서 설정

        `when`(rq.actorFromDb).thenReturn(loggedInMember)
        `when`(memberService.genAccessToken(loggedInMember)).thenReturn(accessToken)
        `when`(request.getParameter("state")).thenReturn(encodedState)

        // WHEN
        handler.onAuthenticationSuccess(request, response, authentication)

        // THEN
        verify(rq).sendRedirect(stateRedirectUrl)
    }
}