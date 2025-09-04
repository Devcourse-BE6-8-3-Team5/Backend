package com.back.domain.news.today.repository

import com.back.domain.news.real.entity.QRealNews
import com.back.domain.news.today.entity.QTodayNews
import com.back.domain.news.today.entity.TodayNews
import com.back.domain.quiz.detail.entity.QDetailQuiz
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class TodayNewsRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory
) : TodayNewsRepositoryCustom {

    private val qTodayNews = QTodayNews.todayNews
    private val qRealNews = QRealNews.realNews
    private val qDetailQuiz = QDetailQuiz.detailQuiz

    override fun findQBySelectedDateWithDetailQuizzes(selectedDate: LocalDate): TodayNews? {
        return jpaQueryFactory
            .select(qTodayNews)
            .from(qTodayNews)
            .join(qTodayNews.realNews(), qRealNews).fetchJoin()
            .leftJoin(qDetailQuiz).on(qDetailQuiz.realNews().eq(qRealNews)).fetchJoin()
            .where(qTodayNews.selectedDate.eq(selectedDate))
            .fetchOne()
    }

    override fun findQByIdWithDetailQuizzes(id: Long): TodayNews? {
        return jpaQueryFactory
            .select(qTodayNews)
            .from(qTodayNews)
            .join(qTodayNews.realNews(), qRealNews).fetchJoin()
            .leftJoin(qDetailQuiz).on(qDetailQuiz.realNews().eq(qRealNews)).fetchJoin()
            .where(qTodayNews.id.eq(id))
            .fetchOne()
    }

    override fun findQTopByOrderBySelectedDateDescWithDetailQuizzes(): TodayNews? {
        return jpaQueryFactory
            .select(qTodayNews)
            .from(qTodayNews)
            .join(qTodayNews.realNews(), qRealNews).fetchJoin()
            .leftJoin(qDetailQuiz).on(qDetailQuiz.realNews().eq(qRealNews)).fetchJoin()
            .orderBy(qTodayNews.selectedDate.desc())
            .limit(1)
            .fetchOne()
    }
}