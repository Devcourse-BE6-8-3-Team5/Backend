package com.back.domain.news.common.dto

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.dto.RealNewsDto

data class AnalyzedNewsDto(
    val realNewsDto: RealNewsDto,
    val score: Int,
    val category: NewsCategory
) {
    constructor(dto: AnalyzedNewsDto) : this(
        realNewsDto = RealNewsDto(dto),
        score = dto.score,
        category = dto.category
    )
}

