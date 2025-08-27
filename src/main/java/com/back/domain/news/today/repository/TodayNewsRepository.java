package com.back.domain.news.today.repository;

import com.back.domain.news.today.entity.TodayNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

public interface TodayNewsRepository extends JpaRepository<TodayNews, Long> {

    @Modifying(clearAutomatically = true)
    @Transactional
    void deleteBySelectedDate(LocalDate today);

    Optional<TodayNews> findBySelectedDate(LocalDate today);

    Optional<TodayNews> findTopByOrderBySelectedDateDesc();
}
