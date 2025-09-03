package com.back.domain.news.common.repository

import com.back.domain.news.common.entity.KeywordHistory
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface KeywordHistoryRepository : JpaRepository<KeywordHistory, Long>, KeywordHistoryRepositoryCustom {

    fun findByUsedDateGreaterThanEqual(startDate: LocalDate): List<KeywordHistory>
}
