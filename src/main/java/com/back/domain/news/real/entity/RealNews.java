package com.back.domain.news.real.entity;


import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Table(name = "real_news", indexes = {
        // 윈도우 함수용 복합 인덱스
        @Index(name = "idx_real_news_category_created_date",
                columnList = "news_category, created_date DESC"),

        // 기본 날짜 정렬용
        @Index(name = "idx_real_news_created_date_desc",
                columnList = "created_date DESC"),

        // 관리자 all API용
        @Index(name = "idx_real_news_origin_created_date_desc",
                columnList = "origin_created_date DESC"),

        @Index(name = "idx_real_news_category_origin_created_date_desc",
                columnList = "news_category, origin_created_date DESC")

})
@NoArgsConstructor
public class RealNews {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String title;

    @Lob
    private String content;

    private String link;
    private String imgUrl;
    private String description;
    private LocalDateTime originCreatedDate; // 원본 뉴스 생성 날짜

    private String originalNewsUrl;  // 원본 뉴스 url
    private String mediaName; // 뉴스 매체 이름 (예: BBC, CNN 등)
    private String journalist; // 뉴스 작성자 (기자)

    // 상세 퀴즈와 1:N 관계 설정 (RealNews 하나 당 3개의 DetailQuiz가 생성됩니다.)
    @OneToMany(mappedBy = "realNews", cascade = ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DetailQuiz> detailQuizzes = new ArrayList<>();

    @OneToMany(mappedBy = "realNews", cascade = ALL, orphanRemoval = true)
    @JsonIgnore
    private List<FactQuiz> factQuizzes = new ArrayList<>();

    @OneToOne(mappedBy = "realNews", cascade = ALL, fetch = LAZY)
    @JsonIgnore
    private FakeNews fakeNews;

    @Enumerated(EnumType.STRING)
    private NewsCategory newsCategory;


    @Column(updatable = false)
    private LocalDateTime createdDate; // 수동 관리

    @Builder
    public RealNews(
            //제목 유니크처리
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
            NewsCategory newsCategory) {
        this.title = title;
        this.content = content;
        this.description = description;
        this.link = link;
        this.imgUrl = imgUrl;
        this.originCreatedDate = originCreatedDate;
        this.createdDate = createdDate;
        this.mediaName = mediaName;
        this.journalist = journalist;
        this.originalNewsUrl = originalNewsUrl;
        this.newsCategory = newsCategory;
    }

}
