package com.back.domain.news.fake.repository

import com.back.domain.news.fake.entity.QFakeNews
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class FakeNewsRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
) : FakeNewsRepositoryCustom {

    private val qFakeNews = QFakeNews.fakeNews

    override fun findExistingIds(ids: List<Long>): List<Long> =
        ids.takeIf { it.isNotEmpty() }
            ?.let {
                jpaQueryFactory
                    .select(qFakeNews.id)
                    .from(qFakeNews)
                    .where(qFakeNews.id.`in`(it))
                    .fetch()
            } ?: emptyList()
}