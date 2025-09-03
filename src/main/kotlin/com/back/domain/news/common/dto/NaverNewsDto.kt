package com.back.domain.news.common.dto


data class NaverNewsDto(
    val title: String?,
    val originallink: String,
    val link: String,
    val description: String?,
    val pubDate: String
) {
    constructor(dto: NaverNewsDto): this(
        title = dto.title,
        originallink = dto.originallink,
        link = dto.link,
        description = dto.description,
        pubDate = dto.pubDate
    )
}