package com.back.domain.news.common.dto;

import com.fasterxml.jackson.databind.JsonNode;

//네이버 API로 조회한 뉴스의 링크 정보를 바탕으로 원본 링크에서 추출할 정보들을 담은 DTO
public record NewsDetailDto(
        String content,      // 본문 내용
        String imgUrl,       // 이미지 URL
        String journalist,   // 기자명
        String mediaName     // 언론사명
) {

    public static NewsDetailDto of(String content, String imgUrl, String journalist, String mediaName) {
        return new NewsDetailDto(content, imgUrl, journalist, mediaName);
    }

    public static NewsDetailDto from(JsonNode item) {
        return new NewsDetailDto(
                item.get("content").asText(""),
                item.get("imgUrl").asText(""),
                item.get("journalist").asText(""),
                item.get("mediaName").asText("")
        );
    }
}