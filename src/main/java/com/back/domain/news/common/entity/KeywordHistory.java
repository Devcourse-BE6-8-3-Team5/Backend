package com.back.domain.news.common.entity;

import com.back.domain.news.common.dto.KeywordWithType;
import com.back.domain.news.common.enums.KeywordType;
import com.back.domain.news.common.enums.NewsCategory;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.*;

@Getter
@Entity
@NoArgsConstructor
public class KeywordHistory {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String keyword;

    @Enumerated(EnumType.STRING)
    private KeywordType keywordType;

    @Enumerated(EnumType.STRING)
    private NewsCategory category;

    private LocalDate usedDate; // 키워드가 사용된 날짜

    private LocalDateTime createAt; // 키워드가 생성된 시간

    private int useCount = 1;

    public void incrementUseCount() {
        this.useCount++;
    }

    @Builder
    public KeywordHistory(
            String keyword,
            KeywordType keywordType,
            NewsCategory category,
            LocalDate usedDate
            ){
        this.keyword = keyword;
        this.keywordType = keywordType;
        this.category = category;
        this.usedDate = usedDate;
        this.createAt = LocalDateTime.now();
    }

}
