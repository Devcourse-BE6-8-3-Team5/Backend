package com.back.domain.news.common.dto

import com.fasterxml.jackson.annotation.JsonProperty


data class KeywordGenerationResDto(
    @JsonProperty("society") val society: List<KeywordWithType> = emptyList(),
    @JsonProperty("economy") val economy: List<KeywordWithType> = emptyList(),
    @JsonProperty("politics") val politics: List<KeywordWithType> = emptyList(),
    @JsonProperty("culture") val culture: List<KeywordWithType> = emptyList(),
    @JsonProperty("it") val it: List<KeywordWithType> = emptyList()
) {

    val keywords: List<String>
        get() = listOf(society, economy, politics, culture, it)
            .flatten()
            .map { it.keyword }
}