package com.back.domain.news.common.service

import com.back.domain.news.common.enums.NewsType
import com.back.global.rsData.RsData
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class NewsPageService {
    fun <T> getPagedNews(
        newsPage: Page<T>,
        newsType: NewsType
    ): RsData<Page<T>?> {
        return when {
            newsPage.totalPages == 0 -> RsData.of(404, "${newsType.description} 뉴스가 없습니다")
            newsPage.isEmpty -> RsData.of(400, "요청한 페이지의 범위 초과. 총 ${newsPage.totalPages}페이지 중 ${newsPage.number + 1}페이지를 요청.")
            else -> RsData.of(200, "${newsPage.numberOfElements}건 조회(전체 ${newsPage.totalElements}건) [ ${newsPage.number + 1} / ${newsPage.totalPages} pages]", newsPage)
        }
    }

    fun <T> getSingleNews(
        news: T?,
        newsType: NewsType,
        id: Long
    ): RsData<T?> {
        return if (news != null) {
            RsData.of(200, "${id}번 ${newsType.description} 뉴스 조회", news)
        } else {
            RsData.of(404, "ID ${id}번에 해당하는 ${newsType.description} 뉴스가 존재하지 않습니다. 올바른 ID를 확인해주세요")
        }
    }
}