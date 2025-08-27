package com.back.domain.news.real.dto;

import com.back.domain.news.common.enums.NewsCategory;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record RealNewsDto(
        Long id,
        String title,
        String content,
        String description,
        String link,
        String imgUrl,
        LocalDateTime originCreatedDate,
        LocalDateTime createdDate,
        String mediaName,
        String journalist,
        String originalNewsUrl,
        NewsCategory newsCategory
) {
    public static RealNewsDto of(
            Long id,
            String title,
            String content,
            String description,
            String link,
            String imgUrl,
            LocalDateTime originCreatedDate,
            LocalDateTime createdDate,
            String mediaName,
            String journalist,
            String originalNewsUrl,
            NewsCategory newsCategory
    ) {
        return new RealNewsDto(
                id, title, content, description, link, imgUrl, originCreatedDate, createdDate, mediaName, journalist, originalNewsUrl, newsCategory
        );
    }

}



