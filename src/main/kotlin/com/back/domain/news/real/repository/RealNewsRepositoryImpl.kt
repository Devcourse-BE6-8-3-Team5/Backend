package com.back.domain.news.real.repository

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.fake.entity.QFakeNews
import com.back.domain.news.real.entity.QRealNews
import com.back.domain.news.real.entity.RealNews
import com.back.domain.quiz.detail.entity.QDetailQuiz
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class RealNewsRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
) : RealNewsRepositoryCustom {

    private val qRealNews = QRealNews.realNews

    override fun findQByTitleExcludingNthCategoryRank(title: String, excludedId: Long, excludedRank: Int, pageable: Pageable): Page<RealNews> {
        // 1. 카테고리별 N번째 뉴스들의 ID를 먼저 조회 (excludedId는 제외하고)
        val excludedIds = NewsCategory.entries.mapNotNull { category ->
            jpaQueryFactory
                .select(qRealNews.id)
                .from(qRealNews)
                .where(
                    qRealNews.newsCategory.eq(category)
                        .and(qRealNews.id.ne(excludedId))
                )
                .orderBy(qRealNews.createdDate.desc())
                .offset((excludedRank - 1).toLong())
                .limit(1)
                .fetchOne()
        }

        // 2. 제목 검색하되 위에서 찾은 ID들과 excludedId 제외
        val allExcludedIds = excludedIds + excludedId

        val total = jpaQueryFactory
            .select(qRealNews.count())
            .from(qRealNews)
            .where(
                qRealNews.title.containsIgnoreCase(title)
                    .and(qRealNews.id.notIn(allExcludedIds))
            )
            .fetchOne() ?: 0L

        val content = jpaQueryFactory
            .selectFrom(qRealNews)
            .where(
                qRealNews.title.containsIgnoreCase(title)
                    .and(qRealNews.id.notIn(allExcludedIds))
            )
            .orderBy(qRealNews.createdDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, total)
    }

    override fun findQAllExcludingNth(
        excludedId: Long,
        excludedRank: Int,
        pageable: Pageable
    ): Page<RealNews> {
        // 1. 각 카테고리별 N번째 뉴스 ID들 조회
        val excludedIds = NewsCategory.entries.mapNotNull { category ->
            jpaQueryFactory
                .select(qRealNews.id)
                .from(qRealNews)
                .where(
                    qRealNews.newsCategory.eq(category)
                        .and(qRealNews.id.ne(excludedId))
                )
                .orderBy(qRealNews.createdDate.desc())
                .offset((excludedRank - 1).toLong())
                .limit(1)
                .fetchOne()
        }

        // 2. 전체 조회하되 위에서 찾은 ID들과 excludedId 제외
        val allExcludedIds = excludedIds + excludedId

        val total = jpaQueryFactory
            .select(qRealNews.count())
            .from(qRealNews)
            .where(qRealNews.id.notIn(allExcludedIds))
            .fetchOne() ?: 0L

        val content = jpaQueryFactory
            .selectFrom(qRealNews)
            .where(qRealNews.id.notIn(allExcludedIds))
            .orderBy(qRealNews.createdDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, total)
    }

    override fun findQByCategoryExcludingNth(
        category: NewsCategory,
        excludedId: Long,
        excludedRank: Int,
        pageable: Pageable
    ): Page<RealNews> {
        // 1. 해당 카테고리에서 N번째 뉴스 ID 조회
        val excludedNthId = jpaQueryFactory
            .select(qRealNews.id)
            .from(qRealNews)
            .where(
                qRealNews.newsCategory.eq(category)
                    .and(qRealNews.id.ne(excludedId))
            )
            .orderBy(qRealNews.createdDate.desc())
            .offset((excludedRank - 1).toLong())
            .limit(1)
            .fetchOne()

        // 2. 카테고리별 조회하되 N번째와 excludedId 제외
        val excludeIds = listOfNotNull(excludedId, excludedNthId)

        val total = jpaQueryFactory
            .select(qRealNews.count())
            .from(qRealNews)
            .where(
                qRealNews.newsCategory.eq(category)
                    .and(qRealNews.id.notIn(excludeIds))
            )
            .fetchOne() ?: 0L

        val content = jpaQueryFactory
            .selectFrom(qRealNews)
            .where(
                qRealNews.newsCategory.eq(category)
                    .and(qRealNews.id.notIn(excludeIds))
            )
            .orderBy(qRealNews.createdDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, total)
    }

    override fun findQNthRankByAllCategories(targetRank: Int): List<RealNews> {
        return NewsCategory.entries.mapNotNull { category ->
            jpaQueryFactory
                .selectFrom(qRealNews)
                .where(qRealNews.newsCategory.eq(category))
                .orderBy(qRealNews.createdDate.desc())
                .offset((targetRank - 1).toLong())
                .limit(1)
                .fetchOne()
        }.sortedByDescending { it.createdDate }
    }

    override fun findQNthRankByCategory(
        category: NewsCategory,
        targetRank: Int
    ): RealNews? {
        return jpaQueryFactory
            .selectFrom(qRealNews)
            .where(qRealNews.newsCategory.eq(category))
            .orderBy(qRealNews.createdDate.desc())
            .offset((targetRank - 1).toLong())
            .limit(1)
            .fetchOne()
    }

    override fun findQByIdWithFakeNews(id: Long): RealNews? {
        return jpaQueryFactory
            .select(qRealNews)
            .from(qRealNews)
            .leftJoin(QFakeNews.fakeNews).on(QFakeNews.fakeNews.id.eq(qRealNews.id)).fetchJoin()
            .where(qRealNews.id.eq(id))
            .fetchOne()
    }

    override fun findQByIdWithDetailQuizzes(id: Long): RealNews? {
        val qDetailQuiz = QDetailQuiz.detailQuiz

        return jpaQueryFactory
            .select(qRealNews).distinct()
            .from(qRealNews)
            .leftJoin(qRealNews.detailQuizzes, qDetailQuiz).fetchJoin()
            .where(qRealNews.id.eq(id))
            .fetchOne()
    }


    override fun findQAllByIdsWithFakeNews(ids: List<Long>): List<RealNews> {
        if (ids.isEmpty()) return emptyList()
        
        return jpaQueryFactory
            .selectFrom(qRealNews)
            .leftJoin(QFakeNews.fakeNews).on(QFakeNews.fakeNews.id.eq(qRealNews.id)).fetchJoin()
            .where(qRealNews.id.`in`(ids))
            .fetch()
    }

}