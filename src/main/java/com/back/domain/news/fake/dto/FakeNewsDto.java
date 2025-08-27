package com.back.domain.news.fake.dto;

public record FakeNewsDto (
        Long realNewsId,
        String content
) {
    public static FakeNewsDto of(Long realNewsId, String content) {
        return new FakeNewsDto(realNewsId, content);
    }
}
