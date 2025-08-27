package com.back.backend.domain.quiz.daily.controller;

import com.back.backend.global.config.TestRqConfig;
import com.back.backend.global.rq.TestRq;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.quiz.detail.service.DetailQuizService;
import com.back.global.rq.Rq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "GEMINI_API_KEY=api_key"
})
@Import(TestRqConfig.class)
public class DailyQuizControllerTest {
    @Autowired
    private DetailQuizService detailQuizService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Rq rq;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        Member testUser = memberService.join("testUser", "12341234", "test@test.com");

        // 테스트 Rq에 사용자 지정
        ((TestRq) rq).setActor(testUser);
    }

    @Test
    @DisplayName("GET /api/quiz/daily/{newsId} - 오늘의 뉴스 ID로 오늘의 퀴즈 목록 조회 성공")
    void t1() throws Exception {
        //Given
        Long newsId = 2L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/daily/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDailyQuizzes"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("오늘의 퀴즈 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].dailyQuizDto.question").value("뉴스에서 소개하는 여름 제철 간식은 무엇인가요?"))
                .andExpect(jsonPath("$.data[1].dailyQuizDto.question").value("뉴스에서 언급된 장소는 어디인가요?"))
                .andExpect(jsonPath("$.data[2].dailyQuizDto.question").value("뉴스에서 감자와 찰옥수수를 소개하는 사람들은 누구인가요?"))
                .andExpect(jsonPath("$.data[0].dailyQuizDto.option1").isString())
                .andExpect(jsonPath("$.data[0].dailyQuizDto.option2").isString())
                .andExpect(jsonPath("$.data[0].dailyQuizDto.option3").isString())
                .andExpect(jsonPath("$.data[0].dailyQuizDto.correctOption").exists())

                .andExpect(jsonPath("$.data[0].answer").doesNotExist())
                .andExpect(jsonPath("$.data[0].gainExp").exists())
                .andExpect(jsonPath("$.data[0].correct").exists())
                .andExpect(jsonPath("$.data[0].quizType").value("DAILY"));

    }

    @Test
    @DisplayName("GET /api/quiz/daily/{newsId} - 오늘의 뉴스 ID로 오늘의 퀴즈 목록 조회 실패 - 뉴스에 퀴즈가 없는 경우")
    void t2() throws Exception {
        //Given
        Long newsId = 1L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/daily/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(handler().methodName("getDailyQuizzes"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("오늘의 뉴스에 해당하는 오늘의 퀴즈가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("POST /api/quiz/daily/submit/{id} - 퀴즈 정답 제출")
    void t3() throws Exception {
        // Given
        Long quizId = 1L;
        String selectedOption = "OPTION2";

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/daily/submit/{id}", quizId)
                        .param("selectedOption", selectedOption))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("submitDailyQuizAnswer"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
                .andExpect(jsonPath("$.data.quizId").value(quizId))
                .andExpect(jsonPath("$.data.selectedOption").value("OPTION2"))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.gainExp").value(10))
                .andExpect(jsonPath("$.data.quizType").value("DAILY"));
    }

    @Test
    @DisplayName("POST /api/quiz/daily/submit/{id} - 퀴즈 오답 제출")
    void t4() throws Exception {
        // Given
        Long quizId = 1L;
        String selectedOption = "OPTION1";

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/daily/submit/{id}", quizId)
                        .param("selectedOption", selectedOption))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("submitDailyQuizAnswer"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
                .andExpect(jsonPath("$.data.quizId").value(quizId))
                .andExpect(jsonPath("$.data.selectedOption").value("OPTION1"))
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.gainExp").value(0))
                .andExpect(jsonPath("$.data.quizType").value("DAILY"));
    }
}
