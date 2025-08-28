package com.back.domain.news.common.dto

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.dto.RealNewsDto

data class AnalyzedNewsDto(
    val realNewsDto: RealNewsDto,
    val score: Int,
    val category: NewsCategory
)
    // {
//    companion object {
//        fun of(realNewsDto: RealNewsDto?, score: Int?, category: NewsCategory?): AnalyzedNewsDto {
//            return AnalyzedNewsDto(realNewsDto, score, category)
//        }
//    }
//}

