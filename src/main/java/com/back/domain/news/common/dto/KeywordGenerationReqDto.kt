package com.back.domain.news.common.dto

import java.time.LocalDate


data class KeywordGenerationReqDto(
    val currentDate: LocalDate,  // db 조회해서 최근 거 뺴거나 일정 기준을 둬서 제외할 키워드
    val recentKeywordsWithTypes: MutableList<String>,
    val excludeKeywords: MutableList<String>
)
