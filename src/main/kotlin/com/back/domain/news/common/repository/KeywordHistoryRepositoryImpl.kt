package com.back.domain.news.common.repository

import com.back.domain.news.common.entity.KeywordHistory
import com.back.domain.news.common.entity.QKeywordHistory
import com.back.domain.news.common.enums.NewsCategory
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class KeywordHistoryRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
) : KeywordHistoryRepositoryCustom {

    private val qKeywordHistory = QKeywordHistory.keywordHistory

        /**
         * 특정 기간 이후 임계값 이상 사용된 키워드 조회
         */
        override fun findOverusedKeywords(startDate: LocalDate, threshold: Int): List<String> {
            return jpaQueryFactory
                .select(qKeywordHistory.keyword)
                .from(qKeywordHistory)
                .where(
                    startDate.let { qKeywordHistory.usedDate.goe(it) }
                )
                .groupBy(qKeywordHistory.keyword)
                .having(qKeywordHistory.keyword.count().goe(threshold.toLong()))
                .fetch()
                .filterNotNull()
        }

        /**
         * 특정 날짜에 사용된 키워드들 조회(해당 날짜만)
         */
        override fun findKeywordsByUsedDate(date: LocalDate): List<String> {
            return jpaQueryFactory
                .selectDistinct(qKeywordHistory.keyword)
                .from(qKeywordHistory)
                .where(
                    date.let { qKeywordHistory.usedDate.eq(it) }
                )
                .fetch()
                .filterNotNull()
        }

        /**
         * 특정 날짜 이전의 키워드 기록 삭제
         */
        override fun deleteByUsedDateBefore(cutoffDate: LocalDate): Long {
            return jpaQueryFactory
                .delete(qKeywordHistory)
                .where(
                    cutoffDate.let { qKeywordHistory.usedDate.lt(it) }
                )
                .execute()
        }

        /**
         * 키워드 목록, 카테고리, 사용일자로 키워드 기록 조회
         */
       override fun findByKeywordsAndCategoryAndUsedDate(
            keywords: List<String>,
            category: NewsCategory,
            usedDate: LocalDate
        ): List<KeywordHistory> {
            return jpaQueryFactory
                .selectFrom(qKeywordHistory)
                .where(
                    keywords.takeIf { it.isNotEmpty() }?.let {
                        qKeywordHistory.keyword.`in`(it)
                    },
                    category.let { qKeywordHistory.category.eq(it) },
                    usedDate.let { qKeywordHistory.usedDate.eq(it) }
                )
                .fetch()
        }
}