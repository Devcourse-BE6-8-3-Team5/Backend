package com.back.domain.news.common.enums;

import lombok.Getter;

@Getter
public enum NewsCategory {
    SOCIETY("사회"),
    ECONOMY("경제"),
    POLITICS("정치"),
    CULTURE("문화"),
    IT("IT"),
    NOT_FILTERED("필터링 전");

    private final String description;

    NewsCategory(String description) {
        this.description = description;
    }
}
