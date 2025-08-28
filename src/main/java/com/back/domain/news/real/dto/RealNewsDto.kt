package com.back.domain.news.real.dto

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
)



