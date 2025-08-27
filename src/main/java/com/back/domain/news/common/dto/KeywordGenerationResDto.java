package com.back.domain.news.common.dto;

import java.util.List;
import java.util.stream.Stream;

public record KeywordGenerationResDto(
        List<KeywordWithType> society,
        List<KeywordWithType> economy,
        List<KeywordWithType> politics,
        List<KeywordWithType> culture,
        List<KeywordWithType> it
) {
    public List<String> getKeywords() {
        return Stream.of(society, economy, politics, culture, it)
                .flatMap(List::stream)
                .map(KeywordWithType::keyword)
                .toList();
    }
}
