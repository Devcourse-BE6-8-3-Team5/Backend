package com.back.domain.member.service

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.AuthTokenService
import com.back.domain.member.member.service.MemberService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
class MemberServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var authTokenService: AuthTokenService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var memberService: MemberService

    @BeforeEach
    fun setUp() {
        memberService = MemberService(memberRepository, authTokenService, passwordEncoder)
    }

    @Test
    @DisplayName("modifyOrJoin: 새로운 소셜 사용자는 회원가입 처리된다.")
    fun testModifyOrJoin_newSocialUser() {
        // GIVEN
        val oauthId = "new_social_id"
        val email = "new@example.com"
        val nickname = "새로운소셜유저"

        // memberRepository.findByOauthId가 Optional.empty()를 반환하도록 Stubbing (새로운 사용자)
        `when`(memberRepository.findByOauthId(oauthId)).thenReturn(Optional.empty())
        // passwordEncoder.encode가 더미 값을 반환하도록 Stubbing
        `when`(passwordEncoder.encode(Mockito.anyString())).thenReturn("encoded_password")

        val newMember = Member(
            name = nickname,
            email = email,
            password = "encoded_password",
            exp = 0,
            level = 1,
            role = "USER",
            apikey = "new-api-key",
            oauthId = oauthId
        )
        // memberRepository.save가 newMember를 반환하도록 Stubbing
        `when`(memberRepository.save(Mockito.any(Member::class.java))).thenReturn(newMember)

        // WHEN
        val rsData = memberService.modifyOrJoin(oauthId, email, nickname)

        // THEN
        assertThat(rsData.code).isEqualTo(201)
        assertThat(rsData.message).isEqualTo("회원가입이 완료되었습니다.")
        assertThat(rsData.code >= 200 && rsData.code < 400).isTrue()
        assertThat(rsData.data).isEqualTo(newMember)

        // findByOauthId가 두 번 호출되었는지 검증 (modifyOrJoin에서 1번, joinSocial에서 1번)
        Mockito.verify(memberRepository, Mockito.times(2)).findByOauthId(oauthId)
        // save가 호출되었는지 검증
        Mockito.verify(memberRepository).save(Mockito.any(Member::class.java))
        // passwordEncoder.encode가 호출되었는지 검증
        Mockito.verify(passwordEncoder).encode(Mockito.anyString())
    }

    @Test
    @DisplayName("modifyOrJoin: 기존 소셜 사용자는 회원 정보가 수정된다.")
    fun testModifyOrJoin_existingSocialUser() {
        // GIVEN
        val oauthId = "existing_social_id"
        val email = "existing@example.com"
        val oldNickname = "기존닉네임"
        val newNickname = "새로운닉네임"

        val existingMember = Member(
            name = oldNickname,
            email = email,
            password = "encoded_password",
            exp = 0,
            level = 1,
            role = "USER",
            apikey = "existing-api-key",
            oauthId = oauthId
        )
        // memberRepository.findByOauthId가 existingMember를 반환하도록 Stubbing (기존 사용자)
        `when`(memberRepository.findByOauthId(oauthId)).thenReturn(Optional.of(existingMember))
        // memberRepository.save가 existingMember를 반환하도록 Stubbing
        `when`(memberRepository.save(Mockito.any(Member::class.java))).thenReturn(existingMember)

        // WHEN
        val rsData = memberService.modifyOrJoin(oauthId, email, newNickname)

        // THEN
        assertThat(rsData.code).isEqualTo(200)
        assertThat(rsData.message).isEqualTo("회원 정보가 수정되었습니다.")
        assertThat(rsData.code >= 200 && rsData.code < 400).isTrue()
        assertThat(rsData.data).isEqualTo(existingMember)
        assertThat(existingMember.name).isEqualTo(newNickname) // 닉네임이 업데이트되었는지 확인

        // findByOauthId가 한 번 호출되었는지 검증
        Mockito.verify(memberRepository, Mockito.times(1)).findByOauthId(oauthId)
        // save가 호출되었는지 검증
        Mockito.verify(memberRepository).save(Mockito.any(Member::class.java))
        // passwordEncoder.encode가 호출되지 않았는지 검증 (회원가입이 아니므로)
        Mockito.verify(passwordEncoder, Mockito.never()).encode(Mockito.anyString())
    }

    @Test
    @DisplayName("genAccessToken: 멤버 객체로 AccessToken을 생성한다.")
    fun testGenAccessToken() {
        // GIVEN
        val member = Member(
            name = "test",
            email = "test@example.com",
            password = "encoded",
            exp = 0,
            level = 1,
            role = "USER",
            apikey = "test-api-key",
            oauthId = "oauth-id"
        )
        val expectedToken = "generated-access-token"

        // authTokenService.genAccessToken이 expectedToken을 반환하도록 Stubbing
        `when`(authTokenService.genAccessToken(member)).thenReturn(expectedToken)

        // WHEN
        val actualToken = memberService.genAccessToken(member)

        // THEN
        assertThat(actualToken).isEqualTo(expectedToken)
        // authTokenService.genAccessToken이 호출되었는지 검증
        Mockito.verify(authTokenService).genAccessToken(member)
    }
}
