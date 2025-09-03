package com.back.domain.quiz.fact.controller

import com.back.domain.member.member.service.MemberService
import com.back.global.config.TestRqConfig
import com.back.global.rq.Rq
import com.back.global.rq.TestRq
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@TestPropertySource(
    properties = [
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url"
    ]
)
@Import(TestRqConfig::class)
class FactQuizControllerTest {
    @Autowired
    private lateinit var memberService: MemberService
    @Autowired
    private lateinit var mvc: MockMvc
    @Autowired
    private lateinit var rq: Rq

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성
        val testUser = memberService.join("testUser", "12341234", "test@test.com")

        // 테스트 Rq에 사용자 지정
        (rq as TestRq).setActor(testUser)
    }

    @Test
    @DisplayName("GET /api/quiz/detail - 팩트 퀴즈 목록 조회")
    fun t1() {
        //Given

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/fact")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("getFactQuizzes"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("팩트 퀴즈 목록 조회 성공")) // TestInitData에 맞춰 설정한 기댓값
            .andExpect(jsonPath("$.data.length()").value(3))
    }

    @Test
    @DisplayName("GET /api/quiz/fact/category - 뉴스 카테고리별 팩트 퀴즈 목록 조회 성공")
    fun t2() {
        //Given
        val category = "IT"

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/fact/category")
                .param("category", category) // IT 카테고리로 조회
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("getFactQuizzesByCategory"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("팩트 퀴즈 목록 조회 성공. 카테고리: $category"))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").isNumber())
            .andExpect(jsonPath("$.data[0].realNewsTitle").value("인도 최대 IT 서비스 TCS 1만2200명 감원 계획"))
            .andExpect(jsonPath("$.data[0].question").isString())
    }

    @Test
    @DisplayName("GET /api/quiz/fact/{id} - 팩트 퀴즈 단건 조회 성공")
    fun t4() {
        //Given
        val quizId = 3L

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/fact/$quizId")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("getFactQuizById"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("팩트 퀴즈 조회 성공. ID: $quizId"))
            .andExpect(jsonPath("$.data.factQuizDto.id").isNumber())
            .andExpect(jsonPath("$.data.factQuizDto.question").isString())
            .andExpect(jsonPath("$.data.factQuizDto.realNewsTitle").value("경제계 \"상법·노조법 개정안 국회 급물살, 우려 넘어 참담\""))
            .andExpect(jsonPath("$.data.factQuizDto.realNewsContent", Matchers.startsWith("한경협·대한상의 등 경제8단체 공동입장문 \"국회, 연이은 규제 입법…")))
            .andExpect(jsonPath("$.data.factQuizDto.fakeNewsContent",
                    Matchers.startsWith("국회에서 논의 중이던 상법 및 노동조합법 개정안이 경제계의 거센 반발에도")
                )
            )
            .andExpect(jsonPath("$.data.factQuizDto.correctNewsType").exists())
            .andExpect(jsonPath("$.data.factQuizDto.quizType").value("FACT"))

            .andExpect(jsonPath("$.data.answer").doesNotExist())
            .andExpect(jsonPath("$.data.gainExp").exists())
            .andExpect(jsonPath("$.data.isCorrect").exists())
    }

    @Test
    @DisplayName("GET /api/quiz/fact/{id} - 팩트 퀴즈 단건 조회 실패 - 존재하지 않는 ID")
    fun t5() {
        //Given
        val quizId = 999L

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/fact/$quizId")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(handler().methodName("getFactQuizById"))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("팩트 퀴즈를 찾을 수 없습니다. ID: $quizId"))
    }

    @Test
    @DisplayName("DELETE /api/quiz/fact/{id} - 팩트 퀴즈 삭제 성공")
    fun t6() {
        //Given
        val quizId = 1L

        //When
        val resultActions = mvc.perform(
            delete("/api/quiz/fact/{id}", quizId)
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("deleteFactQuiz"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("팩트 퀴즈 삭제 성공. ID: $quizId"))
    }

    @Test
    @DisplayName("DELETE /api/quiz/fact/{id} - 팩트 퀴즈 삭제 실패 - 존재하지 않는 ID")
    fun t7() {
        //Given
        val quizId = 999L

        //When
        val resultActions = mvc.perform(
            delete("/api/quiz/fact/{id}", quizId)
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(handler().methodName("deleteFactQuiz"))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("팩트 퀴즈를 찾을 수 없습니다. ID: $quizId"))
    }

    @Test
    @DisplayName("POST /api/quiz/fact/submit/{id} - 퀴즈 정답 제출")
    fun t8() {
        // Given
        val quizId = 1L
        val selectedNewsType = "REAL"

        // When
        val resultActions = mvc.perform(
            post("/api/quiz/fact/submit/{id}", quizId)
                .param("selectedNewsType", selectedNewsType)
        )
            .andDo(print())

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("submitFactQuizAnswer"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
            .andExpect(jsonPath("$.data.quizId").value(quizId))
            .andExpect(jsonPath("$.data.selectedNewsType").value("REAL"))
            .andExpect(jsonPath("$.data.correctNewsType").exists())
            .andExpect(jsonPath("$.data.isCorrect").isBoolean())
            .andExpect(jsonPath("$.data.gainExp").isNumber())
            .andExpect(jsonPath("$.data.quizType").value("FACT"))
    }
}
