package com.back.domain.news.real.controller

import com.back.domain.member.member.entity.Member
import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.entity.RealNews
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.global.config.TestRqConfig
import com.back.global.rq.Rq
import com.back.global.rq.TestRq
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
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
        "HEALTHCHECK_URL=healthcheck-url",
    ]
)
@Import(TestRqConfig::class)
class AdminNewsControllerTest {
    @Autowired
    private lateinit var realNewsRepository: RealNewsRepository

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var rq: Rq

    private lateinit var savedNews1: RealNews
    private lateinit var savedNews2: RealNews
    private lateinit var savedTodayNews: RealNews

    @BeforeEach
    fun setUp() {
        val admin = Member(1, "admin@123", "admin", "ADMIN")
        (rq as TestRq).actor = admin

        savedNews1 = realNewsRepository.save(
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

        savedNews2 = realNewsRepository.save(
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

        savedTodayNews = realNewsRepository.save(
            RealNews(
                title = "TodayNews Title",
                content = "This is TodayNews content.",
                link = "http://example.com/news/3",
                imgUrl = "http://example.com/news/3/image.jpg",
                description = "Today news description.",
                originCreatedDate = LocalDateTime.now(),
                createdDate = LocalDateTime.now().minusDays(3),
                originalNewsUrl = "http://example.com/original/news/3",
                mediaName = "Test Media",
                journalist = "Test Journalist",
                newsCategory = NewsCategory.IT
            )
        )
    }


    @Test
    @DisplayName("GET /api/news/{newsId} - 뉴스 단건 조회 성공")
    fun t1() {
        //Given
        mvc.get("/api/news/${savedNews1.id}") {
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(savedNews1.id) }
            jsonPath("$.data.createdDate") {
                value(startsWith(savedNews1.createdDate.toString().substring(0, 20)))
            }
            jsonPath("$.data.title") { value(savedNews1.title) }
            jsonPath("$.data.link") { value(savedNews1.link) }
            jsonPath("$.data.originalNewsUrl") { value(savedNews1.originalNewsUrl) }
        }
    }

    @Test
    @DisplayName("GET /api/news/{newsId} - 뉴스 단건 조회 실패")
    fun t2() {
        //Given
        val newsId = 999L

        // when & then
        mvc.get("/api/news/$newsId") {
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(404) }
        }
    }

    @Test
    @DisplayName("GET /api/news/today - 오늘의 뉴스 조회 성공")
    fun t3() {
        // when & then
        mvc.get("/api/news/today") {
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
        }
    }


    @Test
    @DisplayName("GET /api/news/all - 관리자용 모든 뉴스 조회")
    @Throws(Exception::class)
    fun t4() {
        // when & then
        mvc.get("/api/admin/news/all") {
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
        }
    }

    @Test
    @DisplayName("GET /api/news/search/{title} - 검색 조회")
    fun t5() {
        // when & then
        mvc.get("/api/news/search") {
            param("title", "Test")
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            // 실제 결과 확인을 위해 단순화
        }
    }

    @Test
    @DisplayName("GET /api/news/category/{category} - 카테고리별 뉴스 조회")
    fun t6() {
        // when & then
        mvc.get("/api/news/category/{category}", "IT") {
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
        }
    }

    @Test
    @DisplayName("PUT /api/admin/news/today/select/{newsId} - 오늘의 뉴스 설정 변경 성공")
    fun t7() {
        // when & then
        mvc.put("/api/admin/news/today/select/${savedTodayNews.id}") {
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data.id") { value(savedTodayNews.id) }
        }
    }

    @Test
    @DisplayName("PUT /api/admin/news/today/select/{newsId} - 존재하지 않는 뉴스 ID로 오늘의 뉴스 설정 실패")
    fun t8() {
        val nonExistentId = 999L
        
        mvc.put("/api/admin/news/today/select/$nonExistentId") {
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(404) }
            jsonPath("$.message") { value("ID ${nonExistentId}에 해당하는 뉴스가 존재하지 않습니다") }
        }
    }

    @Test
    @DisplayName("DELETE /api/admin/news/{newsId} - 뉴스 삭제 성공")
    fun t9() {
        mvc.delete("/api/admin/news/${savedNews2.id}") {
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.message") { value("${savedNews2.id}번 뉴스 삭제 완료") }
            jsonPath("$.data") { value("삭제 성공") }
        }
    }

    @Test
    @DisplayName("DELETE /api/admin/news/{newsId} - 존재하지 않는 뉴스 삭제 실패")
    fun t10() {
        val nonExistentId = 999L
        
        mvc.delete("/api/admin/news/$nonExistentId") {
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(404) }
            jsonPath("$.message") { value("ID ${nonExistentId}에 해당하는 뉴스가 존재하지 않습니다") }
            jsonPath("$.data") { value("삭제 실패") }
        }
    }

    // 배치 처리 테스트는 AI API 의존성으로 인해 제외
    // 별도의 통합 테스트나 모킹을 통한 단위 테스트에서 처리

    @Test
    @DisplayName("GET /api/admin/news/{newsId} - 관리자용 단건 뉴스 조회 성공")
    fun t12() {
        mvc.get("/api/admin/news/${savedNews1.id}") {
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data.id") { value(savedNews1.id) }
            jsonPath("$.data.title") { value(savedNews1.title) }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news/{newsId} - 잘못된 ID로 단건 뉴스 조회 실패")
    fun t13() {
        val invalidId = -1L
        
        mvc.get("/api/admin/news/$invalidId") {
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("잘못된 뉴스 ID입니다. 1 이상의 숫자를 입력해주세요.") }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news/today - 관리자용 오늘의 뉴스 조회")
    fun t14() {
        mvc.get("/api/admin/news/today") {
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news - 관리자용 뉴스 목록 조회")
    fun t15() {
        mvc.get("/api/admin/news") {
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news - 잘못된 페이지 파라미터로 뉴스 목록 조회 실패")
    fun t16() {
        mvc.get("/api/admin/news") {
            param("page", "0") // 잘못된 페이지 번호
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("페이지 번호는 1 이상이어야 합니다") }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news/search - 관리자용 뉴스 검색")
    fun t17() {
        mvc.get("/api/admin/news/search") {
            param("title", "Test")
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news/search - 빈 검색어로 검색 실패")
    fun t18() {
        mvc.get("/api/admin/news/search") {
            param("title", "") // 빈 검색어
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("검색어를 입력해주세요") }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news/category/{category} - 관리자용 카테고리별 뉴스 조회")
    fun t19() {
        mvc.get("/api/admin/news/category/IT") {
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news/category/{category} - 잘못된 카테고리로 조회 실패")
    fun t20() {
        mvc.get("/api/admin/news/category/INVALID_CATEGORY") {
            param("page", "1")
            param("size", "10")
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value(startsWith("올바르지 않은 카테고리입니다")) }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news/all - 잘못된 크기 파라미터로 모든 뉴스 조회 실패")
    fun t21() {
        mvc.get("/api/admin/news/all") {
            param("page", "1")
            param("size", "101") // 최대 크기 초과
            param("direction", "desc")
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("페이지 크기는 1~100 사이여야 합니다") }
        }
    }

    @Test
    @DisplayName("GET /api/admin/news - 잘못된 정렬 방향으로 뉴스 목록 조회 실패")
    fun t22() {
        mvc.get("/api/admin/news") {
            param("page", "1")
            param("size", "10")
            param("direction", "invalid") // 잘못된 정렬 방향
        }.andDo {
            print()
        }.andExpect {
            status { is4xxClientError() }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("정렬 방향은 'asc' 또는 'desc'여야 합니다") }
        }
    }
}


