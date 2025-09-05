package com.back.domain.news.real.service

import com.back.domain.news.common.dto.NewsDetailDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class NewsCrawlingService {

    companion object {
        private val log = LoggerFactory.getLogger(NewsCrawlingService::class.java)
    }
    // 단건 크롤링
    fun crawlAdditionalInfo(naverNewsUrl: String): NewsDetailDto? {
        return runCatching {
            val doc = Jsoup.connect(naverNewsUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()

            val content = doc.selectFirst("article#dic_area")?.let { extractTextWithLineBreaks(it) } ?: ""
            val imgUrl = doc.selectFirst("#img1")?.attr("data-src") ?: ""
            val journalist = doc.selectFirst("em.media_end_head_journalist_name")?.text() ?: ""
            val mediaName = doc.selectFirst("img.media_end_head_top_logo_img")?.attr("alt") ?: ""

            // 모든 정보가 있는 경우만 반환
            if (listOf(content, imgUrl, journalist, mediaName).all { it.isNotBlank() }) {
                NewsDetailDto(content, imgUrl, journalist, mediaName)
            } else null
        }.onFailure { e ->
            if (e is IOException) {
                log.warn("크롤링 실패: {}", naverNewsUrl)
            }
        }.getOrNull()
    }

    private fun extractTextWithLineBreaks(element: Element): String {
        element.select("p").before("\n\n")
        element.select("div").before("\n\n")
        element.select("br").before("\n")
        return element.text().replace("\\n", "\n")
    }

}