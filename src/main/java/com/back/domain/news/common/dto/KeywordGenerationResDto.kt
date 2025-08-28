package com.back.domain.news.common.dto

import java.util.stream.Stream


data class KeywordGenerationResDto(
    @JvmField val society: MutableList<KeywordWithType?>?,
    @JvmField val economy: MutableList<KeywordWithType?>?,
    @JvmField val politics: MutableList<KeywordWithType?>?,
    @JvmField val culture: MutableList<KeywordWithType?>?,
    @JvmField val it: MutableList<KeywordWithType?>?
) {
    val keywords: MutableList<String?>
        get() = Stream.of<MutableList<KeywordWithType?>?>(
            society,
            economy,
            politics,
            culture,
            it
        )
            .flatMap<KeywordWithType?> { obj: MutableList<KeywordWithType?>? -> obj!!.stream() }
            .map<String?>(KeywordWithType::keyword)
            .toList()
}
