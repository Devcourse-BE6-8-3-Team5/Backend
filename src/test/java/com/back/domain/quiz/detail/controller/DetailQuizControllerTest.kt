package com.back.domain.quiz.detail.controller

import com.back.domain.member.member.service.MemberService
import com.back.domain.quiz.detail.dto.DetailQuizDto
import com.back.domain.quiz.detail.entity.Option
import com.back.global.config.TestRqConfig
import com.back.global.rq.Rq
import com.back.global.rq.TestRq
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
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
    properties = ["NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "HEALTHCHECK_URL=health_check_url",
        "GEMINI_API_KEY=gemini_api_key"
    ]
)
@Import(TestRqConfig::class)
class DetailQuizControllerTest {
    @Autowired
    private lateinit var memberService: MemberService

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
    @DisplayName("GET /api/quiz/detail/{id} - 상세 퀴즈 단건 조회 성공")
    fun t2() {
        //Given
        val quizId = 5L

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/detail/$quizId")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("getDetailQuiz"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("상세 퀴즈 조회 성공"))
            .andExpect(jsonPath("$.data.detailQuizResDto.question").value("뉴스에서 언급된 장소는 어디인가요?"))
            .andExpect(jsonPath("$.data.detailQuizResDto.option1").value("서울 강남구 농협유통 하나로마트"))
            .andExpect(jsonPath("$.data.detailQuizResDto.option2").value("서울 서초구 농협유통 하나로마트 양재점"))
            .andExpect(jsonPath("$.data.detailQuizResDto.option3").value("경기도 성남시 농협유통 하나로마트"))
            .andExpect(jsonPath("$.data.detailQuizResDto.correctOption").value("OPTION2"))

            .andExpect(jsonPath("$.data.answer").doesNotExist())
            .andExpect(jsonPath("$.data.gainExp").exists())
            .andExpect(jsonPath("$.data.isCorrect").exists())
            .andExpect(jsonPath("$.data.quizType").value("DETAIL"))
    }

    @Test
    @DisplayName("GET /api/quiz/detail/{id} - 상세 퀴즈 단건 조회 실패 - 존재하지 않는 ID")
    fun t3() {
        //Given
        val quizId = 999L

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/detail/$quizId")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(handler().methodName("getDetailQuiz"))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("해당 id의 상세 퀴즈가 존재하지 않습니다. id: $quizId"))
    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회 성공")
    fun t4() {
        //Given
        val newsId = 2L

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/detail/news/$newsId")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("뉴스 ID로 상세 퀴즈 목록 조회 성공"))
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].question").value("뉴스에서 소개하는 여름 제철 간식은 무엇인가요?"))
            .andExpect(jsonPath("$.data[1].question").value("뉴스에서 언급된 장소는 어디인가요?"))
            .andExpect(jsonPath("$.data[2].question").value("뉴스에서 감자와 찰옥수수를 소개하는 사람들은 누구인가요?"))
    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회 실패 - 존재하지 않는 뉴스 ID")
    fun t5() {
        //Given
        val newsId = 999L

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/detail/news/$newsId")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("해당 id의 뉴스가 존재하지 않습니다. id: $newsId"))
    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회 실패 - 뉴스에 퀴즈가 없는 경우")
    @Throws(
        Exception::class
    )
    fun t6() {
        //Given
        val newsId = 8L

        //When
        val resultActions = mvc.perform(
            get("/api/quiz/detail/news/$newsId")
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("해당 뉴스에 대한 상세 퀴즈가 존재하지 않습니다. newsId: $newsId"))
    }

    @Test
    @DisplayName("POST /api/quiz/detail/news/{newsId}/regenerate - 뉴스 ID로 상세 퀴즈 생성")
    @Disabled("실제 AI 호출 테스트 - 필요할 때만 실행")
    @Throws(
        Exception::class
    )
    fun t7() {
        // Given
        val newsId = 1L

        // When
        val resultActions =
            mvc.perform(post("/api/quiz/detail/news/{newsId}/regenerate", newsId))
                .andDo(print())

        // Then
        resultActions
            .andExpect(status().isCreated())
            .andExpect(handler().methodName("generateDetailQuizzes"))
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value("상세 퀴즈 생성 성공"))
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].question").isNotEmpty())
    }

    @Test
    @DisplayName("PUT /api/quiz/detail/{id} - 상세 퀴즈 수정")
    @Throws(
        Exception::class
    )
    fun t8() {
        //Given
        val quizId = 1L
        val updatedDto = DetailQuizDto("수정된 질문", "수정된 옵션1", "수정된 옵션2", "수정된 옵션3", Option.OPTION2)

        //When
        val resultActions = mvc.perform(
            put("/api/quiz/detail/{id}", quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto))
        ).andDo(print())

        //Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("updateDetailQuiz"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("상세 퀴즈 수정 성공"))
            .andExpect(jsonPath("$.data.question").value("수정된 질문"))
            .andExpect(jsonPath("$.data.correctOption").value("OPTION2"))
    }

    @Test
    @DisplayName("POST /api/quiz/detail/submit/{id} - 퀴즈 정답 제출")
    @Throws(
        Exception::class
    )
    fun t9() {
        // Given
        val quizId = 1L
        val selectedOption = "OPTION2"

        // When
        val resultActions = mvc.perform(
            post("/api/quiz/detail/submit/{id}", quizId)
                .param("selectedOption", selectedOption)
        )
            .andDo(print())

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("submitDetailQuizAnswer"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
            .andExpect(jsonPath("$.data.quizId").value(quizId))
            .andExpect(jsonPath("$.data.selectedOption").value("OPTION2"))
            .andExpect(jsonPath("$.data.isCorrect").value(true))
            .andExpect(jsonPath("$.data.gainExp").value(10))
            .andExpect(jsonPath("$.data.quizType").value("DETAIL"))
    }

    @Test
    @DisplayName("POST /api/quiz/detail/submit/{id} - 퀴즈 오답 제출")
    @Throws(
        Exception::class
    )
    fun t10() {
        // Given
        val quizId = 1L
        val selectedOption = "OPTION1"

        // When
        val resultActions = mvc.perform(
            post("/api/quiz/detail/submit/{id}", quizId)
                .param("selectedOption", selectedOption)
        )
            .andDo(print())

        // Then
        resultActions
            .andExpect(status().isOk())
            .andExpect(handler().methodName("submitDetailQuizAnswer"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
            .andExpect(jsonPath("$.data.quizId").value(quizId))
            .andExpect(jsonPath("$.data.selectedOption").value("OPTION1"))
            .andExpect(jsonPath("$.data.isCorrect").value(false))
            .andExpect(jsonPath("$.data.gainExp").value(0))
            .andExpect(jsonPath("$.data.quizType").value("DETAIL"))
    }
}
