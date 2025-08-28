package com.back.domain.news.common.entity

import com.back.domain.news.common.enums.KeywordType
import com.back.domain.news.common.enums.NewsCategory
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime


@Entity
data class KeywordHistory(
    var keyword: String,
    @field:Enumerated(EnumType.STRING) var keywordType: KeywordType,
    @field:Enumerated(EnumType.STRING) var category: NewsCategory,
    var usedDate: LocalDate
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(updatable = false)
    var createdAt: LocalDateTime
    var useCount = 1
        private set

    fun incrementUseCount() {
        this.useCount++
    }

    init {
        this.createdAt = LocalDateTime.now()
    }
}
