package com.back.domain.news.common.service

import com.back.domain.news.common.enums.NewsType
import com.back.global.rsData.RsData
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.util.*

@Component
class NewsPageService {
    fun <T> getPagedNews(
        newsPage: Page<T>,
        newsType: NewsType
    ): RsData<Page<T>> {
        val newsTypeDescription: String = newsType.description

        if (newsPage.totalPages == 0) {
            return RsData.of<Page<T>>(404, String.format("%s 뉴스가 없습니다", newsTypeDescription))
        }
        if (newsPage.isEmpty) {
            return RsData.of<Page<T>>(
                400,
                String.format(
                    "요청한 페이지의 범위 초과. 총 %d페이지 중 %d페이지를 요청.",
                    newsPage.totalPages, newsPage.number + 1
                )
            )
        }

        return RsData.of<Page<T>>(
            200,
            String.format(
                "%s 뉴스 %d건 조회(전체 %d건)  [ %d / %d pages]",
                newsType,
                newsPage.numberOfElements,
                newsPage.totalElements,
                newsPage.number + 1,
                newsPage.totalPages
            ),
            newsPage
        )
    }

    fun <T> getSingleNews(
        news: Optional<T>,
        newsType: NewsType,
        id: Long
    ): RsData<T> {
        val newsTypeDescription: String? = newsType.description

        return news
            .map { dto -> RsData.of(200, "${id}번 $newsTypeDescription 뉴스 조회", dto) }
            .orElse(RsData.of(404,"${id}번의 $newsTypeDescription 뉴스가 존재하지 않습니다"))
    }
}
