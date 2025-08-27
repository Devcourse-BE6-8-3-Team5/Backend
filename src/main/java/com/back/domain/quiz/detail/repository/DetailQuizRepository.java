package com.back.domain.quiz.detail.repository;

import com.back.domain.quiz.detail.entity.DetailQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetailQuizRepository extends JpaRepository<DetailQuiz, Long> {
    List<DetailQuiz> findByRealNewsId(Long realNewsId);

    void deleteByRealNewsId(Long newsId);
}
