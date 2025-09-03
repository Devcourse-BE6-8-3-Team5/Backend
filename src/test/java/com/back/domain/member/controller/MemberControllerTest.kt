package com.back.domain.member.controller

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.MemberService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
    fun myInfo_without_auth() {
        mvc.perform(get("/api/members/info"))
            .andDo(print())
            .andExpect(status().isUnauthorized())
    }

    @Test
    @DisplayName("잘못된 accessToken, apiKey로 접근 시 398에러")
    fun myInfo_with_invalid_token() {
        mvc.perform(
            get("/api/members/info")
                .cookie(Cookie("accessToken", "invalid"))
                .cookie(Cookie("apiKey", "invalid"))
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    fun login_fail_email_not_found() {
        mvc.perform(
            post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "notfound@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
                )
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("존재하지 않는 이메일입니다."))
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    fun login_fail_wrong_password() {
        // 회원가입
        mvc.perform(
            post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test6@example.com"
                }
            
            """.trimIndent()
                )
        )

        // 로그인(틀린 비밀번호)
        mvc.perform(
            post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "test6@example.com",
                    "password": "wrongpassword"
                }
            
            """.trimIndent()
                )
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    fun join_fail_duplicate_email() {
        // 첫 번째 회원가입
        mvc.perform(post("/api/members/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test7@example.com"
                }
            
            """.trimIndent()
            )
        )

        // 두 번째 회원가입(같은 이메일)
        mvc.perform(post("/api/members/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "다른유저",
                    "password": "12345678910",
                    "email": "test7@example.com"
                }
            
            """.trimIndent()
            )
        )
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."))
    }

    @Test
    @DisplayName("회원가입 실패 - 유효하지 않은 이메일 형식")
    fun join_fail_invalid_email() {
        mvc.perform(post("/api/members/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "invalidemail"
                }
            
            """.trimIndent()
            )
        )
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    @DisplayName("accessToken만 있고 apiKey가 없을 때 인증 성공")
    fun myInfo_with_only_accessToken() {
        // 회원가입 및 로그인
        mvc.perform(post("/api/members/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test8@example.com"
                }
            
            """.trimIndent()
            )
        )
        val loginResult = mvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "email": "test8@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
            )
        )
        val accessToken = loginResult.andReturn().response.getCookie("accessToken")?.value

        // accessToken만으로 마이페이지 접근
        mvc.perform(get("/api/members/info")
            .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("test8@example.com"))
    }

    @Test
    @DisplayName("apiKey만 있고 accessToken이 없을 때 인증 성공")
    fun myInfo_with_only_apiKey() {
        // 회원가입 및 로그인
        mvc.perform(
            post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test9@example.com"
                }
            
            """.trimIndent()
                )
        )
        val loginResult = mvc.perform(
            post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "email": "test9@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
                )
        )
        val apiKey = loginResult.andReturn().response.getCookie("apiKey")?.value

        // apiKey만으로 마이페이지 접근
        mvc.perform(get("/api/members/info")
            .cookie(Cookie("apiKey", apiKey))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("test9@example.com"))
    }

    @Test
    @DisplayName("로그아웃 후 재로그인 성공")
    fun logout_then_relogin() {
        // 회원가입 및 로그인
        mvc!!.perform(post("/api/members/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "테스트유저",
                    "password": "12345678910",
                    "email": "test10@example.com"
                }
            
            """.trimIndent()
            )
        )
        val loginResult = mvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "email": "test10@example.com",
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
            .andExpect(status().isOk())

        // 재로그인
        mvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "email": "test10@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
            )
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.apiKey").exists())
    }

    @Test
    @DisplayName("일반 유저가 마이페이지 접근 성공")
    fun user_access_myInfo_success() {
        // 일반 유저 회원가입 및 로그인
        mvc.perform(post("/api/members/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "일반유저",
                    "password": "12345678910",
                    "email": "user@example.com"
                }
            
            """.trimIndent()
            )
        )

        val loginResult = mvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "email": "user@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
            )
        )

        val accessToken = loginResult.andReturn().response.getCookie("accessToken")?.value

        // 일반 유저가 마이페이지 접근 (성공해야 함)
        mvc.perform(get("/api/members/info")
            .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("USER"))
    }

    @Test
    @DisplayName("일반 유저가 관리자 페이지 접근 시 403에러")
    fun user_access_admin_page_forbidden() {
        // 일반 유저 회원가입 및 로그인
        mvc.perform(
            post("/api/members/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "name": "일반유저",
                    "password": "12345678910",
                    "email": "user2@example.com"
                }
            
            """.trimIndent()
                )
        )

        val loginResult = mvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "email": "user2@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
            )
        )

        val accessToken = loginResult.andReturn().response.getCookie("accessToken")?.value

        // 일반 유저가 관리자 페이지 접근 (403 에러)
        mvc.perform(
            get("/api/admin/members")
                .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(print())
            .andExpect(status().isForbidden())
    }

    @Test
    @DisplayName("관리자가 마이페이지 접근 성공")
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

        memberRepository.save(adminMember)

        // 관리자 로그인
        val loginResult = mvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "email": "admin2@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
            )
        )

        val accessToken = loginResult.andReturn().response.getCookie("accessToken")?.value

        // 관리자가 마이페이지 접근 (성공해야 함)
        mvc.perform(get("/api/members/info")
            .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
    }

    @Test
    @DisplayName("인증 없이 관리자 페이지 접근 시 401에러")
    fun no_auth_access_admin_page_forbidden() {
        // 인증 없이 관리자 페이지 접근
        mvc.perform(get("/api/admin/members"))
            .andDo(print())
            .andExpect(status().isUnauthorized())
    }

    @Test
    @DisplayName("일반 유저가 관리자 전용 API 접근 시 403에러")
    fun user_access_admin_api_forbidden() {
        // 일반 유저 회원가입 및 로그인
        mvc.perform(post("/api/members/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "일반유저3",
                    "password": "12345678910",
                    "email": "user3@example.com"
                }
            
            """.trimIndent()
            )
        )

        val loginResult = mvc.perform(post("/api/members/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "email": "user3@example.com",
                    "password": "12345678910"
                }
            
            """.trimIndent()
            )
        )

        val accessToken = loginResult.andReturn().response.getCookie("accessToken")?.value

        // 일반 유저가 관리자 전용 API 접근 (403 에러)
        mvc.perform(delete("/api/admin/members")
            .cookie(Cookie("accessToken", accessToken))
        )
            .andDo(print())
            .andExpect(status().isForbidden())
    }
}