package com.back.domain.news.common.enums;

import lombok.Getter;

@Getter
public enum NewsType {
    REAL("진짜"),
    FAKE("가짜");

    private final String description;

    NewsType(String description) {
        this.description = description;
    }

}
