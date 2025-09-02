package com.back.backend.domain.member.controller

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.MemberService
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
    properties = ["NAVER_CLIENT_ID=test_client_id_for_testing_only",
        "NAVER_CLIENT_SECRET=test_client_secret_for_testing_only",
        "HEALTHCHECK_URL=https://hc-ping.com/8bbf100d-5404-4c5e-a172-516985b353fe"
    ]
)
class MemberControllerTest @Autowired constructor(
    private val memberService: MemberService,
    private val memberRepository: MemberRepository,
    private val mvc: MockMvc
){

    @Test
    @DisplayName("회원가입 성공")
    fun join_success() {
        mvc.perform(post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "name": "테스트유저",
                        "password": "12345678910",
                        "email": "test@example.com"
                    }
                
                """.trimIndent()
                )
        )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.data.name").value("테스트유저"))
            .andExpect(jsonPath("$.data.email").value("test@example.com"))
    }

    @Test
    @DisplayName("로그인 성공 및 쿠키에 accessToken, apiKey 포함")
    fun login_success() {
        // 회원가입
        mvc.perform(post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "name": "테스트유저",
                        "password": "12345678910",
                        "email": "test2@example.com"
                    }
                
                """.trimIndent()
                )
        )

        // 로그인
        val loginResult = mvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "email": "test2@example.com",
                        "password": "12345678910"
                    }
                
                """.trimIndent()
                )
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.apiKey").exists())

        // 쿠키 확인
        val response = loginResult.andReturn().response
        val accessToken = response.getCookie("accessToken")?.value
        val apiKey = response.getCookie("apiKey")?.value
        assertTrue(accessToken != null && !accessToken.isBlank())
        assertTrue(apiKey != null && !apiKey.isBlank())
    }

    @Test
    @DisplayName("accessToken으로 인증된 마이페이지 접근 성공")
    fun myInfo_with_accessToken() {
        // 회원가입 및 로그인
        mvc.perform(post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "name": "테스트유저",
                        "password": "12345678910",
                        "email": "test3@example.com"
                    }
                
                """.trimIndent()
                )
        )
        val loginResult = mvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "email": "test3@example.com",
                        "password": "12345678910"
                    }
                
                """.trimIndent()
                )
        )
        val response = loginResult.andReturn().response
        val accessToken = response.getCookie("accessToken")?.value

        // accessToken으로 마이페이지 접근
        mvc.perform(get("/api/members/info")
                .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("test3@example.com"))
    }

    @Test
    @DisplayName("accessToken 만료 시 apiKey로 accessToken 재발급")
    fun accessToken_expired_then_reissue_with_apiKey() {
        // 회원가입 및 로그인
        mvc.perform(post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "name": "테스트유저",
                        "password": "12345678910",
                        "email": "test4@example.com"
                    }
                
                """.trimIndent()
                )
        )
        val loginResult = mvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "email": "test4@example.com",
                        "password": "12345678910"
                    }
                
                """.trimIndent()
                )
        )
        val response = loginResult.andReturn().response
        val apiKey = response.getCookie("apiKey")?.getValue()

        // 만료된 accessToken 사용 (임의의 잘못된 토큰)
        val expiredToken = "expired.jwt.token"

        // 만료된 accessToken + 유효한 apiKey로 마이페이지 접근 시 accessToken 재발급
        mvc.perform(get("/api/members/info")
                .cookie(Cookie("accessToken", expiredToken))
                .cookie(Cookie("apiKey", apiKey))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("test4@example.com"))
    }

    @Test
    @DisplayName("로그아웃 시 쿠키만 삭제, DB의 apiKey는 남아있음")
    fun logout_only_cookie_deleted() {
        // 회원가입 및 로그인
        mvc.perform(post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "name": "테스트유저",
                        "password": "12345678910",
                        "email": "test5@example.com"
                    }
                
                """.trimIndent()
                )
        )
        val loginResult = mvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "email": "test5@example.com",
                        "password": "12345678910"
                    }
                
                """.trimIndent()
                )
        )
        val response = loginResult.andReturn().response
        val apiKey = response.getCookie("apiKey")?.value
        val accessToken = response.getCookie("accessToken")?.value

        // 로그아웃
        mvc.perform(delete("/api/members/logout")
                .cookie(Cookie("accessToken", accessToken))
                .cookie(Cookie("apiKey", apiKey))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("로그아웃 성공"))

        // DB의 apiKey는 여전히 존재
        val member = memberService.findByEmail("test5@example.com").get()
        assertTrue(!member.apiKey.isBlank())
    }

    @Test
    @DisplayName("인증 없이 마이페이지 접근 시 403에러")
    @Throws(Exception::class)
    fun myInfo_without_auth() {
        mvc!!.perform(MockMvcRequestBuilders.get("/api/members/info"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    @DisplayName("잘못된 accessToken, apiKey로 접근 시 398에러")
    @Throws(Exception::class)
    fun myInfo_with_invalid_token() {
        mvc!!.perform(
            MockMvcRequestBuilders.get("/api/members/info")
                .cookie(Cookie("accessToken", "invalid"))
                .cookie(Cookie("apiKey", "invalid"))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    @Throws(Exception::class)
    fun login_fail_email_not_found() {
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "notfound@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("존재하지 않는 이메일입니다."))
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    @Throws(Exception::class)
    fun login_fail_wrong_password() {
        // 회원가입
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test6@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        // 로그인(틀린 비밀번호)
        mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "test6@example.com",
                    "password": "wrongpassword"
                }
            
            """.trimIndent().stripIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    @Throws(Exception::class)
    fun join_fail_duplicate_email() {
        // 첫 번째 회원가입
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test7@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        // 두 번째 회원가입(같은 이메일)
        mvc.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "다른유저",
                    "password": "12345678910",
                    "email": "test7@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 존재하는 이메일입니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 유효하지 않은 이메일 형식")
    @Throws(Exception::class)
    fun join_fail_invalid_email() {
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "invalidemail"
                }
            
            """.trimIndent().stripIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    @Test
    @DisplayName("accessToken만 있고 apiKey가 없을 때 인증 성공")
    @Throws(Exception::class)
    fun myInfo_with_only_accessToken() {
        // 회원가입 및 로그인
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test8@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )
        val loginResult = mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "test8@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )
        val accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue()

        // accessToken만으로 마이페이지 접근
        mvc.perform(
            MockMvcRequestBuilders.get("/api/members/info")
                .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value("test8@example.com"))
    }

    @Test
    @DisplayName("apiKey만 있고 accessToken이 없을 때 인증 성공")
    @Throws(Exception::class)
    fun myInfo_with_only_apiKey() {
        // 회원가입 및 로그인
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test9@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )
        val loginResult = mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "test9@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )
        val apiKey = loginResult.andReturn().getResponse().getCookie("apiKey").getValue()

        // apiKey만으로 마이페이지 접근
        mvc.perform(
            MockMvcRequestBuilders.get("/api/members/info")
                .cookie(Cookie("apiKey", apiKey))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value("test9@example.com"))
    }

    @Test
    @DisplayName("로그아웃 후 재로그인 성공")
    @Throws(Exception::class)
    fun logout_then_relogin() {
        // 회원가입 및 로그인
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test10@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )
        val loginResult = mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "test10@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )
        val apiKey = loginResult.andReturn().getResponse().getCookie("apiKey").getValue()
        val accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue()

        // 로그아웃
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/members/logout")
                .cookie(Cookie("accessToken", accessToken))
                .cookie(Cookie("apiKey", apiKey))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())

        // 재로그인
        mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "test10@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.apiKey").exists())
    }

    @Test
    @DisplayName("일반 유저가 마이페이지 접근 성공")
    @Throws(Exception::class)
    fun user_access_myInfo_success() {
        // 일반 유저 회원가입 및 로그인
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "일반유저",
                    "password": "12345678910",
                    "email": "user@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        val loginResult = mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "user@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        val accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue()

        // 일반 유저가 마이페이지 접근 (성공해야 함)
        mvc.perform(
            MockMvcRequestBuilders.get("/api/members/info")
                .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("USER"))
    }

    @Test
    @DisplayName("일반 유저가 관리자 페이지 접근 시 403에러")
    @Throws(Exception::class)
    fun user_access_admin_page_forbidden() {
        // 일반 유저 회원가입 및 로그인
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "일반유저",
                    "password": "12345678910",
                    "email": "user2@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        val loginResult = mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "user2@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        val accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue()

        // 일반 유저가 관리자 페이지 접근 (403 에러)
        mvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members")
                .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @Test
    @DisplayName("관리자가 마이페이지 접근 성공")
    @Throws(Exception::class)
    fun admin_access_myInfo_success() {
        // 관리자 회원 생성

        val adminMember = Member(
            "관리자",
            "admin2@example.com",
            "$2a$10\$uLw2UPuzvGo5IebUw4pV9uetx9re5IBiedKPAmJkF/X6puaajxuA2",
            0,
            1,
            "ADMIN",
            UUID.randomUUID().toString(),
            null

        )

        memberRepository!!.save<Member?>(adminMember)

        // 관리자 로그인
        val loginResult = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "admin2@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        val accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue()

        // 관리자가 마이페이지 접근 (성공해야 함)
        mvc.perform(
            MockMvcRequestBuilders.get("/api/members/info")
                .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("ADMIN"))
    }

    @Test
    @DisplayName("인증 없이 관리자 페이지 접근 시 401에러")
    @Throws(Exception::class)
    fun no_auth_access_admin_page_forbidden() {
        // 인증 없이 관리자 페이지 접근
        mvc!!.perform(MockMvcRequestBuilders.get("/api/admin/members"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    @Test
    @DisplayName("일반 유저가 관리자 전용 API 접근 시 403에러")
    @Throws(Exception::class)
    fun user_access_admin_api_forbidden() {
        // 일반 유저 회원가입 및 로그인
        mvc!!.perform(
            MockMvcRequestBuilders.post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "일반유저3",
                    "password": "12345678910",
                    "email": "user3@example.com"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        val loginResult = mvc.perform(
            MockMvcRequestBuilders.post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "user3@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent().stripIndent()
                )
        )

        val accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue()

        // 일반 유저가 관리자 전용 API 접근 (403 에러)
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/admin/members")
                .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
    }
}