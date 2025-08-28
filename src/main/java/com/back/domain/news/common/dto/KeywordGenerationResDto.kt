package com.back.domain.news.common.dto


data class KeywordGenerationResDto(
    val society: List<KeywordWithType>,
    val economy: List<KeywordWithType>,
    val politics: List<KeywordWithType>,
    val culture: List<KeywordWithType>,
    val it: List<KeywordWithType>
) {
    val keywords: List<String>
        get() = listOf(society, economy, politics, culture, it)
            .flatten()
            .map { it.keyword }
}