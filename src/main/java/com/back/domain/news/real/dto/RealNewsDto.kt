package com.back.domain.news.real.dto

import com.back.domain.news.common.dto.AnalyzedNewsDto
import com.back.domain.news.common.enums.NewsCategory
import java.time.LocalDateTime

data class RealNewsDto(
    val id: Long,
    val title: String,
    val content: String,
    val description: String,
    val link: String,
    val imgUrl: String,
    val originCreatedDate: LocalDateTime,
    val createdDate: LocalDateTime,
    val mediaName: String,
    val journalist: String,
    val originalNewsUrl: String,
    val newsCategory: NewsCategory
) {
    constructor(dto: AnalyzedNewsDto) : this(
        id = dto.realNewsDto.id,
        title = dto.realNewsDto.title,
        content = dto.realNewsDto.content,
        description = dto.realNewsDto.description,
        link = dto.realNewsDto.link,
        imgUrl = dto.realNewsDto.imgUrl,
        originCreatedDate = dto.realNewsDto.originCreatedDate,
        createdDate = dto.realNewsDto.createdDate,
        mediaName = dto.realNewsDto.mediaName,
        journalist = dto.realNewsDto.journalist,
        originalNewsUrl = dto.realNewsDto.originalNewsUrl,
        newsCategory = dto.realNewsDto.newsCategory
    )
}




