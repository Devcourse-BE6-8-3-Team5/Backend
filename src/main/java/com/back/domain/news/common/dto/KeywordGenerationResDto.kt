package com.back.domain.news.common.dto


data class KeywordGenerationResDto(
    val society: List<KeywordWithType> = emptyList(),
    val economy: List<KeywordWithType> = emptyList(),
    val politics: List<KeywordWithType> = emptyList(),
    val culture: List<KeywordWithType> = emptyList(),
    val it: List<KeywordWithType> = emptyList()
) {

    // 모든 키워드를 String 리스트로 반환
    val keywords: List<String>
        get() = getAllKeywordLists()
            .flatten()
            .map { it.keyword }

    // 모든 KeywordWithType 리스트들을 반환 (내부 재사용)
    private fun getAllKeywordLists(): List<List<KeywordWithType>> =
        listOf(society, economy, politics, culture, it)
}