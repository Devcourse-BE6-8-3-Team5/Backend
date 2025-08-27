package com.back.domain.news.common.dto;

import com.back.domain.news.common.enums.KeywordType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record KeywordWithType(
        String keyword,

        @Enumerated(EnumType.STRING)
        KeywordType keywordType
) {
}
