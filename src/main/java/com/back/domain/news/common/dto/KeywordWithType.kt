package com.back.domain.news.common.dto

import com.back.domain.news.common.enums.KeywordType
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class KeywordWithType(
        val keyword: String?,
        @field:Enumerated(EnumType.STRING) val keywordType: KeywordType
)
