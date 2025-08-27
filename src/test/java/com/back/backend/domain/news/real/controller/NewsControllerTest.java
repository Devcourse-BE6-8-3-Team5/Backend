package com.back.backend.domain.news.real.controller;


import com.back.backend.global.config.TestRqConfig;
import com.back.backend.global.rq.TestRq;
import com.back.domain.member.member.entity.Member;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.global.rq.Rq;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
public class NewsControllerTest {

    @Autowired
    private RealNewsRepository realNewsRepository;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Rq rq;

    @BeforeEach
    void setUp() {
        Member admin = new Member( 1, "admin@123", "admin",  "ADMIN");
        ((TestRq) rq).setActor(admin);

        realNewsRepository.save(RealNews.builder()
                .title("Test News Title")
                .content("This is a test news content.")
                .link("http://example.com/news/1")
                .imgUrl("http://example.com/news/1/image.jpg")
                .description("Test news description.")
                .originCreatedDate(java.time.LocalDateTime.now())
                .createdDate(LocalDateTime.now().minusDays(5))
                .originalNewsUrl("http://example.com/original/news/1")
                .mediaName("Test Media")
                .journalist("Test Journalist")
                .newsCategory(NewsCategory.IT)
                .build()
        );

        realNewsRepository.save(RealNews.builder()
                .title("Test News Title2")
                .content("This is a test news content.")
                .link("http://example.com/news/1")
                .imgUrl("http://example.com/news/1/image.jpg")
                .description("Test news description.")
                .originCreatedDate(java.time.LocalDateTime.now())
                .createdDate(LocalDateTime.now().minusDays(5))
                .originalNewsUrl("http://example.com/original/news/1")
                .mediaName("Test Media")
                .journalist("Test Journalist")
                .newsCategory(NewsCategory.IT)
                .build()
        );

    }


    @Test
    @DisplayName("GET /api/news/{newsId} - 뉴스 단건 조회 성공")
    void t1() throws Exception {
        //Given
        long newsId = 1L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/news/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getRealNewsById"))
                .andExpect(jsonPath("$.code").value(200));

    }

    @Test
    @DisplayName("GET /api/news/{newsId} - 뉴스 단건 조회 실패")
    void t2() throws Exception {
        //Given
        long newsId = 999L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/news/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(handler().methodName("getRealNewsById"))
                .andExpect(jsonPath("$.code").value(404));

    }

    @Test
    @DisplayName("GET /api/news/today - 오늘의 뉴스 조회 성공")
    void t3() throws Exception {
        //When
        ResultActions resultActions = mvc.perform(get("/api/news/today")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getTodayNews"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("조회 성공"));

    }


    @Test
    @DisplayName("GET /api/news/all - 관리자용 모든 뉴스 조회")
    void t4() throws Exception {
        //Given

        //When
        ResultActions resultActions = mvc.perform(
                get("/api/admin/news/all")
                        .param("page", "1")
                        .param("size", "10")
                        .param("direction", "desc")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getAllRealNewsList"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/news/search/{title} - 검색 조회")
    void t5() throws Exception {

        //When
        ResultActions resultActions = mvc.perform(
                get("/api/news/search")
                        .param("title", "Test")
                        .param("page", "1")
                        .param("size", "10")
                        .param("direction", "desc")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("searchRealNewsByTitle"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/news/category/{category} - 카테고리별 뉴스 조회")
    void t6() throws Exception {
        //Given

        //When
        ResultActions resultActions = mvc.perform(
                get("/api/news/category/{category}", "IT")
                        .param("page", "1")
                        .param("size", "10")
                        .param("direction", "desc")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getRealNewsByCategory"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PUT /api/news/today/select/{newsId} - 오늘의 뉴스 설정 변경 성공")
    void t7() throws Exception {
        //When
        ResultActions resultActions = mvc.perform(
                put("/api/admin/news/today/select/{newsId}", 1L)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("setTodayNews"))
                .andExpect(jsonPath("$.code").value(200));

    }

}


