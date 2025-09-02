package com.back.domain.news.fake.dto

data class FakeNewsDto(
    val realNewsId: Long,
    val content: String
) {
    constructor(fakeNewsDto: FakeNewsDto) : this(
        realNewsId = fakeNewsDto.realNewsId,
        content = fakeNewsDto.content
    )
}
