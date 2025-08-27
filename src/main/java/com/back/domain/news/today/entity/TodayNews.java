package com.back.domain.news.today.entity;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class TodayNews {
    @Id
    private Long id;

    private LocalDate selectedDate;

    @OneToOne
    @MapsId
    @JoinColumn(name = "real_news_id")
    @JsonIgnore
    private RealNews realNews;

    // 오늘의 퀴즈와 1:N 관계 설정
    @OneToMany(mappedBy = "todayNews", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DailyQuiz> todayQuizzes = new ArrayList<>();


    @Builder
    public TodayNews(LocalDate selectedDate, RealNews realNews) {
        this.selectedDate = selectedDate;
        this.realNews = realNews;
    }
}