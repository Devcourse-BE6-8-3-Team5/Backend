package com.back.domain.quiz.daily.service

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.quizhistory.entity.QuizHistory
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository
import com.back.domain.member.quizhistory.service.QuizHistoryService
import com.back.domain.news.today.repository.TodayNewsRepository
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.daily.dto.DailyQuizAnswerDto
import com.back.domain.quiz.daily.dto.DailyQuizDto
import com.back.domain.quiz.daily.dto.DailyQuizWithHistoryDto
import com.back.domain.quiz.daily.entity.DailyQuiz
import com.back.domain.quiz.daily.repository.DailyQuizRepository
import com.back.domain.quiz.detail.entity.Option
import com.back.global.exception.ServiceException
import com.back.global.util.LevelSystem.calculateLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DailyQuizService(
    private val dailyQuizRepository: DailyQuizRepository,
    private val todayNewsRepository: TodayNewsRepository,
    private val memberRepository: MemberRepository,
    private val quizHistoryService: QuizHistoryService,
    private val quizHistoryRepository: QuizHistoryRepository
) {
    private val log = LoggerFactory.getLogger(DailyQuizService::class.java)

    @Transactional(readOnly = true)
    fun getDailyQuizzes(todayNewsId: Long, actor: Member): List<DailyQuizWithHistoryDto> {
        val quizzes = getDailyQuizzesByTodayNews(todayNewsId)

        val quizIds: Set<Long> = quizzes.map { it.id }.toSet()
        val historyMap = getQuizHistoryMapByMemberAndQuizIds(actor, quizIds)

        return quizzes.map { quiz -> convertToDto(quiz, historyMap[quiz.id]) }
    }

    private fun getDailyQuizzesByTodayNews(todayNewsId: Long): List<DailyQuiz> {
        val quizzes = dailyQuizRepository.findByTodayNewsId(todayNewsId)
        if (quizzes.isEmpty()) {
            throw ServiceException(404, "오늘의 뉴스에 해당하는 오늘의 퀴즈가 존재하지 않습니다.")
        }
        return quizzes
    }

    private fun getQuizHistoryMapByMemberAndQuizIds(member: Member, quizIds: Set<Long>): Map<Long, QuizHistory> {
        val histories: List<QuizHistory> = quizHistoryRepository.findByMemberAndQuizTypeAndQuizIdIn(
            member, QuizType.DAILY, quizIds
        )

        return histories
            .filter { it.quizId != null }
            .associateBy { it.quizId!! }
    }

    private fun convertToDto(quiz: DailyQuiz, history: QuizHistory?): DailyQuizWithHistoryDto {
        return DailyQuizWithHistoryDto(
            DailyQuizDto(quiz),
            history?.answer,
            history?.isCorrect ?: false,
            history?.gainExp ?: 0,
            QuizType.DAILY
        )
    }

    @Transactional
    fun createDailyQuiz(todayNewsId: Long) {
        val todayNews = todayNewsRepository.findQByIdWithDetailQuizzes(todayNewsId)
            ?: throw ServiceException(404, "해당 ID의 오늘의 뉴스가 없습니다.")

        if (dailyQuizRepository.existsByTodayNews(todayNews)) {
            throw ServiceException(400, "오늘의 퀴즈가 이미 생성되었습니다.")
        }

        val quizzes = todayNews.realNews.detailQuizzes
        if (quizzes.isEmpty()) {
            throw ServiceException(404, "연결된 상세 퀴즈가 없습니다.")
        }

        quizzes.forEach { quiz ->
            if (dailyQuizRepository.existsByDetailQuiz(quiz)) {
                log.warn("이미 존재하는 DetailQuiz 기반 DailyQuiz입니다. id = {}", quiz.id)
                return@forEach
            }
            todayNews.todayQuizzes.add(DailyQuiz(todayNews, quiz))
        }
    }

    @Transactional
    fun createDailyQuiz() {
        val today = LocalDate.now()
        val todayNews = todayNewsRepository.findQBySelectedDateWithDetailQuizzes(today)
            ?: throw ServiceException(404, "오늘의 뉴스가 존재하지 않습니다.")

        if (dailyQuizRepository.existsByTodayNews(todayNews)) {
            throw ServiceException(400, "오늘의 퀴즈가 이미 생성되었습니다.")
        }

        val quizzes = todayNews.realNews.detailQuizzes
        if (quizzes.isEmpty()) {
            throw ServiceException(404, "연결된 상세 퀴즈가 없습니다.")
        }

        quizzes.forEach { quiz ->
            if (dailyQuizRepository.existsByDetailQuiz(quiz)) {
                log.warn("이미 존재하는 DetailQuiz 기반 DailyQuiz입니다. id = {}", quiz.id)
                return@forEach
            }
            todayNews.todayQuizzes.add(DailyQuiz(todayNews, quiz))
        }
    }

    fun count(): Long {
        return dailyQuizRepository.count()
    }

    @Transactional
    fun submitDetailQuizAnswer(actor: Member, id: Long, selectedOption: Option): DailyQuizAnswerDto {
        val dailyQuiz = dailyQuizRepository.findById(id)
            .orElseThrow {
                ServiceException(404, "오늘의 퀴즈를 찾을 수 없습니다.")
            }

        val managedActor = memberRepository.findById(actor.id)
            .orElseThrow {
                ServiceException(404, "회원이 존재하지 않습니다.")
            }

        val detailQuiz = dailyQuiz.detailQuiz

        val isCorrect = detailQuiz.isCorrect(selectedOption)
        val gainExp = if (isCorrect) 20 else 0

        managedActor.apply {
            exp += gainExp
            level = calculateLevel(exp)
        }

        quizHistoryService.save(
            managedActor,
            id,
            dailyQuiz.quizType,
            selectedOption.toString(),
            isCorrect,
            gainExp
        ) // 퀴즈 히스토리 저장

        return DailyQuizAnswerDto(
            dailyQuiz.id,
            detailQuiz.question,
            detailQuiz.correctOption,
            selectedOption,
            isCorrect,
            gainExp,
            dailyQuiz.quizType
        )
    }
}
