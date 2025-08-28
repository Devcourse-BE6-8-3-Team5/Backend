package com.back.backend.domain.news.real.controller

import com.back.backend.global.config.TestRqConfig
import com.back.backend.global.rq.TestRq
import com.back.domain.member.member.entity.Member
import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.entity.RealNews
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.global.rq.Rq
import org.hamcrest.Matchers.startsWith
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@TestPropertySource(
    properties = [
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "GEMINI_API_KEY=api_key",
        "HEALTHCHECK_URL=abc"
    ]
)
@Import(TestRqConfig::class)
class NewsControllerTest {
    @Autowired
    private lateinit var realNewsRepository: RealNewsRepository

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var rq: Rq

    @BeforeEach
    fun setUp() {
        val admin = Member(1, "admin@123", "admin", "ADMIN")
        (rq as TestRq).setActor(admin)

        realNewsRepository.save(
            RealNews(
                title = "Test News Title",
                content = "This is a test news content.",
                link = "http://example.com/news/1",
                imgUrl = "http://example.com/news/1/image.jpg",
                description = "Test news description.",
                originCreatedDate = LocalDateTime.now(),
                createdDate = LocalDateTime.now().minusDays(5),
                originalNewsUrl = "http://example.com/original/news/1",
                mediaName = "Test Media",
                journalist = "Test Journalist",
                newsCategory = NewsCategory.IT
            )
        )

        realNewsRepository.save(
            RealNews(
                title = "Test News Title2",
                content = "This is a test news2 content.",
                link = "http://example.com/news/2",
                imgUrl = "http://example.com/news/2/image.jpg",
                description = "Test news description2.",
                originCreatedDate = LocalDateTime.now(),
                createdDate = LocalDateTime.now().minusDays(5),
                originalNewsUrl = "http://example.com/original/news/2",
                mediaName = "Test Media",
                journalist = "Test Journalist",
                newsCategory = NewsCategory.IT
            )
        )
    }


    @Test
    @DisplayName("GET /api/news/{newsId} - 뉴스 단건 조회 성공")
    @Throws(Exception::class)
    fun t1() {
        //Given
        val news = realNewsRepository.findAll().first()


        mvc.get("/api/news/${news.id}") {
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(news.id) }
            jsonPath("$.createdDate") {
                value(startsWith(news.createdDate.toString().substring(0, 20)))
            }
            jsonPath("$.title") { value(news.title) }
            jsonPath("$.link") { value(news.link) }
            jsonPath("$.originalNewsUrl") { value(news.originalNewsUrl) }
        }
    }

    @Test
    @DisplayName("GET /api/news/{newsId} - 뉴스 단건 조회 실패")
    @Throws(Exception::class)
    fun t2() {
        //Given
        val newsId = 999L

        //When
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/news/" + newsId)
        ).andDo(MockMvcResultHandlers.print())

        //Then
        resultActions
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(MockMvcResultMatchers.handler().methodName("getRealNewsById"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
    }

    @Test
    @DisplayName("GET /api/news/today - 오늘의 뉴스 조회 성공")
    @Throws(Exception::class)
    fun t3() {
        //When
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/news/today")
        ).andDo(MockMvcResultHandlers.print())

        //Then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().methodName("getTodayNews"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("조회 성공"))
    }


    @Test
    @DisplayName("GET /api/news/all - 관리자용 모든 뉴스 조회")
    @Throws(Exception::class)
    fun t4() {
        //Given

        //When

        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/admin/news/all")
                .param("page", "1")
                .param("size", "10")
                .param("direction", "desc")
        ).andDo(MockMvcResultHandlers.print())

        //Then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().methodName("getAllRealNewsList"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
    }

    @Test
    @DisplayName("GET /api/news/search/{title} - 검색 조회")
    @Throws(Exception::class)
    fun t5() {
        //When

        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/news/search")
                .param("title", "Test")
                .param("page", "1")
                .param("size", "10")
                .param("direction", "desc")
        ).andDo(MockMvcResultHandlers.print())

        //Then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().methodName("searchRealNewsByTitle"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
    }

    @Test
    @DisplayName("GET /api/news/category/{category} - 카테고리별 뉴스 조회")
    @Throws(Exception::class)
    fun t6() {
        //Given

        //When

        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/news/category/{category}", "IT")
                .param("page", "1")
                .param("size", "10")
                .param("direction", "desc")
        ).andDo(MockMvcResultHandlers.print())

        //Then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().methodName("getRealNewsByCategory"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
    }

    @Test
    @DisplayName("PUT /api/news/today/select/{newsId} - 오늘의 뉴스 설정 변경 성공")
    @Throws(Exception::class)
    fun t7() {
        //When
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/admin/news/today/select/{newsId}", 1L)
        ).andDo(MockMvcResultHandlers.print())

        //Then
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().methodName("setTodayNews"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
    }
}


