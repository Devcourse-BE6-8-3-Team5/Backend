package com.back.domain.quiz.fact.service

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository
import com.back.domain.member.quizhistory.service.QuizHistoryService
import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.fake.entity.FakeNews
import com.back.domain.news.real.entity.RealNews
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.fact.dto.FactQuizAnswerDto
import com.back.domain.quiz.fact.dto.FactQuizDto
import com.back.domain.quiz.fact.dto.FactQuizDtoWithNewsContent
import com.back.domain.quiz.fact.dto.FactQuizWithHistoryDto
import com.back.domain.quiz.fact.entity.CorrectNewsType
import com.back.domain.quiz.fact.entity.FactQuiz
import com.back.domain.quiz.fact.repository.FactQuizRepository
import com.back.global.exception.ServiceException
import com.back.global.util.LevelSystem.calculateLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.ThreadLocalRandom

@Service
class FactQuizService(
    private val factQuizRepository: FactQuizRepository,
    private val realNewsRepository: RealNewsRepository,
    private val memberRepository: MemberRepository,
    private val quizHistoryService: QuizHistoryService,
    private val quizHistoryRepository: QuizHistoryRepository
) {
    private val log = LoggerFactory.getLogger(FactQuizService::class.java)

    @Transactional(readOnly = true)
    fun findByRank(rank: Int): List<FactQuizDto> {
        val nthRankNews = realNewsRepository.findQNthRankByAllCategories(rank)

        // 해당 뉴스들의 FactQuiz 조회
        return nthRankNews
            .mapNotNull { realNews ->
                factQuizRepository.findByRealNewsId(realNews.id).orElse(null)
            }
            .map { factQuiz -> FactQuizDto(factQuiz) }

    }

    @Transactional(readOnly = true)
    fun findByCategory(category: NewsCategory, rank: Int): List<FactQuizDto> =
        findByCategoryAndRank(category, rank)
            .map(::listOf)
            .orElseGet(::emptyList)

    @Transactional(readOnly = true)
    fun findByCategoryAndRank(category: NewsCategory, rank: Int): Optional<FactQuizDto> {
        val realNews = realNewsRepository.findQNthRankByCategory(category, rank)
            ?: return Optional.empty()

        return factQuizRepository.findByRealNewsId(realNews.id)
            .map(::FactQuizDto)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long, actor: Member): FactQuizWithHistoryDto {
        val factQuiz = factQuizRepository.findByIdWithNews(id)
            .orElseThrow {
                ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다. ID: $id")
            }

        val factQuizDto = FactQuizDtoWithNewsContent(factQuiz)

        // 필터링: 퀴즈 ID가 일치하고, 퀴즈 타입이 FACT인 히스토리만 추출
        val quizHistory = quizHistoryRepository.findByMember(actor)
            .firstOrNull { it.quizId == id && it.quizType == QuizType.FACT }

        // 퀴즈 히스토리가 없으면 null 반환
        return if (quizHistory == null) {
            FactQuizWithHistoryDto(factQuizDto, null, false, 0)
        } else {
            FactQuizWithHistoryDto(
                factQuizDto,
                quizHistory.answer,
                quizHistory.isCorrect,
                quizHistory.gainExp
            )
        }
    }

    @Transactional
    fun create(realNewsIds: List<Long>) {
        val realNewsList = realNewsRepository.findAllById(realNewsIds)

        if (realNewsList.isEmpty()) {
            throw ServiceException(404, "팩트 퀴즈를 생성할 진짜 뉴스가 존재하지 않습니다. ID 목록: $realNewsIds")
        }

        val quizzes = realNewsList.mapNotNull { news ->
            val fakeNews = news.fakeNews
            if (fakeNews == null) {
                log.warn("가짜 뉴스가 존재하지 않습니다. 진짜 뉴스 ID: ${news.id}")
                null
            } else createQuiz(news, fakeNews)
        }

        /*
        val quizzes = realNewsList.mapNotNull { news ->
            news.fakeNews?.let { createQuiz(news, it) }
                ?: run { log.warn("가짜 뉴스가 존재하지 않습니다. 진짜 뉴스 ID: ${news.id}"); null }
        }
         */

        if (quizzes.isEmpty()) {
            log.warn("가짜 뉴스가 없어 생성된 퀴즈가 없습니다.")
            return
        }

        factQuizRepository.saveAll(quizzes)
        log.info("퀴즈 ${quizzes.size}개 저장 완료")
    }


    @Transactional
    fun delete(id: Long) {
        val quiz = factQuizRepository.findById(id)
            .orElseThrow {
                ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다. ID: $id")
            }

        factQuizRepository.delete(quiz)
    }

    fun count(): Long {
        return factQuizRepository.count()
    }

    // initData 전용
    @Transactional
    fun create(realNewsId: Long) {
        val real = realNewsRepository.findById(realNewsId)
            .orElseThrow {
                ServiceException(404, "진짜 뉴스를 찾을 수 없습니다. ID: $realNewsId")
            }

        val fake = real.fakeNews ?: throw ServiceException(404, "가짜 뉴스가 없습니다. realNewsId=$realNewsId")

        val quiz = createQuiz(real, fake)
        factQuizRepository.save(quiz)

        log.debug("팩트 퀴즈 생성 완료. 퀴즈 ID: ${quiz.id}, 뉴스 ID: ${real.id}")
    }

    private fun createQuiz(real: RealNews, fake: FakeNews): FactQuiz {
        // 퀴즈 질문과 정답은 랜덤으로 생성
        val answerType = if (ThreadLocalRandom.current().nextBoolean())
            CorrectNewsType.REAL
        else
            CorrectNewsType.FAKE

        val question = if (answerType == CorrectNewsType.REAL)
            "다음 중 진짜 뉴스는?"
        else
            "다음 중 가짜 뉴스는?"

        return FactQuiz(question, real, fake, answerType)
    }

    @Transactional
    fun submitDetailQuizAnswer(actor: Member, id: Long, selectedNewsType: CorrectNewsType): FactQuizAnswerDto {
        val factQuiz = factQuizRepository.findById(id)
            .orElseThrow {
                ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다")
            }

        val managedActor = memberRepository.findById(actor.id)
            .orElseThrow {
                ServiceException(404, "회원이 존재하지 않습니다.")
            }

        val isCorrect = factQuiz.correctNewsType == selectedNewsType
        val gainExp = if (isCorrect) 10 else 0

        managedActor.apply {
            exp += gainExp
            level = calculateLevel(exp)
        }

        quizHistoryService.save(
            managedActor,
            id,
            factQuiz.quizType,
            selectedNewsType.toString(),
            isCorrect,
            gainExp
        )

        return FactQuizAnswerDto(
            factQuiz.id,
            factQuiz.question,
            selectedNewsType,
            factQuiz.correctNewsType,
            isCorrect,
            gainExp,
            factQuiz.quizType
        )
    }
}
