package com.back.domain.news.common.dto;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;

public record AnalyzedNewsDto(
    RealNewsDto realNewsDto,
    Integer score,
    NewsCategory category
){
    public static AnalyzedNewsDto of(RealNewsDto realNewsDto, Integer score, NewsCategory category) {
        return new AnalyzedNewsDto(realNewsDto, score, category);
    }
}

