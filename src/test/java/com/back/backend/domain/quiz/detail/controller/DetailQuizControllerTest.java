package com.back.backend.domain.quiz.detail.controller;

import com.back.backend.global.config.TestRqConfig;
import com.back.backend.global.rq.TestRq;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.entity.Option;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
class DetailQuizControllerTest {
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

    private Member testMember;


    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        Member testUser = memberService.join("testUser", "12341234", "test@test.com");

        // 테스트 Rq에 사용자 지정
        ((TestRq) rq).setActor(testUser);
    }

    @Test
    @DisplayName("GET /api/quiz/detail/{id} - 상세 퀴즈 단건 조회 성공")
    void t2() throws Exception {
        //Given
        Long quizId = 5L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/" + quizId)
        ).andDo(print());

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
                .andExpect(jsonPath("$.data.correct").exists())
                .andExpect(jsonPath("$.data.quizType").value("DETAIL"));

    }

    @Test
    @DisplayName("GET /api/quiz/detail/{id} - 상세 퀴즈 단건 조회 실패 - 존재하지 않는 ID")
    void t3() throws Exception {
        //Given
        Long quizId = 999L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/" + quizId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(handler().methodName("getDetailQuiz"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("해당 id의 상세 퀴즈가 존재하지 않습니다. id: " + quizId));

    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회 성공")
    void t4() throws Exception {
        //Given
        Long newsId = 2L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/news/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("뉴스 ID로 상세 퀴즈 목록 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].question").value("뉴스에서 소개하는 여름 제철 간식은 무엇인가요?"))
                .andExpect(jsonPath("$.data[1].question").value("뉴스에서 언급된 장소는 어디인가요?"))
                .andExpect(jsonPath("$.data[2].question").value("뉴스에서 감자와 찰옥수수를 소개하는 사람들은 누구인가요?"));
    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회 실패 - 존재하지 않는 뉴스 ID")
    void t5() throws Exception {
        //Given
        Long newsId = 999L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/news/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("해당 id의 뉴스가 존재하지 않습니다. id: " + newsId));
    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회 실패 - 뉴스에 퀴즈가 없는 경우")
    void t6() throws Exception {
        //Given
        Long newsId = 8L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/news/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("해당 뉴스에 대한 상세 퀴즈가 존재하지 않습니다. newsId: " + newsId));
    }

    @Test
    @DisplayName("POST /api/quiz/detail/news/{newsId}/regenerate - 뉴스 ID로 상세 퀴즈 생성")
    void t7() throws Exception {
        // Given
        Long newsId = 1L;

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/detail/news/{newsId}/regenerate", newsId))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(handler().methodName("generateDetailQuizzes"))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 생성 성공"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].question").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /api/quiz/detail/{id} - 상세 퀴즈 수정")
    void t8() throws Exception {
        //Given
        Long quizId = 1L;
        DetailQuizDto updatedDto = new DetailQuizDto("수정된 질문", "수정된 옵션1", "수정된 옵션2", "수정된 옵션3", Option.OPTION2);

        //When
        ResultActions resultActions = mvc.perform(put("/api/quiz/detail/{id}", quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto))
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("updateDetailQuiz"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 수정 성공"))
                .andExpect(jsonPath("$.data.question").value("수정된 질문"))
                .andExpect(jsonPath("$.data.correctOption").value("OPTION2"));
    }

    @Test
    @DisplayName("POST /api/quiz/detail/submit/{id} - 퀴즈 정답 제출")
    void t9() throws Exception {
        // Given
        Long quizId = 1L;
        String selectedOption = "OPTION2";

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/detail/submit/{id}", quizId)
                        .param("selectedOption", selectedOption))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("submitDetailQuizAnswer"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
                .andExpect(jsonPath("$.data.quizId").value(quizId))
                .andExpect(jsonPath("$.data.selectedOption").value("OPTION2"))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.gainExp").value(10))
                .andExpect(jsonPath("$.data.quizType").value("DETAIL"));
    }

    @Test
    @DisplayName("POST /api/quiz/detail/submit/{id} - 퀴즈 오답 제출")
    void t10() throws Exception {
        // Given
        Long quizId = 1L;
        String selectedOption = "OPTION1";

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/detail/submit/{id}", quizId)
                        .param("selectedOption", selectedOption))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("submitDetailQuizAnswer"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
                .andExpect(jsonPath("$.data.quizId").value(quizId))
                .andExpect(jsonPath("$.data.selectedOption").value("OPTION1"))
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.gainExp").value(0))
                .andExpect(jsonPath("$.data.quizType").value("DETAIL"));
    }
}
