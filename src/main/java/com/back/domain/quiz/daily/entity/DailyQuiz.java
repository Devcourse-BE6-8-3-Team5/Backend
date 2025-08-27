package com.back.domain.quiz.daily.entity;

import com.back.domain.news.today.entity.TodayNews;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DailyQuiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "today_news_id")
    @JsonIgnore
    private TodayNews todayNews;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detail_quiz_id", unique = true)
    @JsonIgnore
    private DetailQuiz detailQuiz;

    @Enumerated(EnumType.STRING)
    @NotNull
    private QuizType quizType = QuizType.DAILY;

    public DailyQuiz(TodayNews todayNews, DetailQuiz detailQuiz) {
        this.todayNews = todayNews;
        this.detailQuiz = detailQuiz;
    }
}
