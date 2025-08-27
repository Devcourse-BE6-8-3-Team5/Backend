package com.back.domain.news.common.dto;

import com.fasterxml.jackson.databind.JsonNode;

//네이버 뉴스 API 응답을 매핑하기 위한 DTO
public record NaverNewsDto(
        String title,
        String originallink,
        String link,
        String description,
        String pubDate
) {
    public static NaverNewsDto of(String title, String originallink, String link, String description, String pubDate) {
        return new NaverNewsDto(title, originallink, link, description, pubDate);
    }

    public static NaverNewsDto from(JsonNode item) {
        return new NaverNewsDto(
                item.get("title").asText(""),
                item.get("originallink").asText(""),
                item.get("link").asText(""),
                item.get("description").asText(""),
                item.get("pubDate").asText("")
        );
    }

}