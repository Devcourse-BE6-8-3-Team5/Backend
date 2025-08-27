package com.back.domain.news.common.dto;

import java.time.LocalDate;
import java.util.List;

public record KeywordGenerationReqDto(
        LocalDate currentDate,
        // db 조회해서 최근 거 뺴거나 일정 기준을 둬서 제외할 키워드
        List<String> recentKeywordsWithTypes,
        List<String> excludeKeywords

) {
}
