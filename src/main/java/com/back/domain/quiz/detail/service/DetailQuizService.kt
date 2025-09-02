package com.back.domain.quiz.detail.service

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.quizhistory.entity.QuizHistory
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository
import com.back.domain.member.quizhistory.service.QuizHistoryService
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.quiz.QuizType
import com.back.domain.quiz.detail.dto.*
import com.back.domain.quiz.detail.entity.DetailQuiz
import com.back.domain.quiz.detail.entity.Option
import com.back.domain.quiz.detail.repository.DetailQuizRepository
import com.back.global.ai.AiService
import com.back.global.ai.processor.DetailQuizProcessor
import com.back.global.exception.ServiceException
import com.back.global.util.LevelSystem.calculateLevel
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DetailQuizService(
    private val detailQuizRepository: DetailQuizRepository,
    private val realNewsRepository: RealNewsRepository,
    private val aiService: AiService,
    private val objectMapper: ObjectMapper,
    private val quizHistoryService: QuizHistoryService,
    private val quizHistoryRepository: QuizHistoryRepository,
    private val memberRepository: MemberRepository
) {
    private val log = LoggerFactory.getLogger(DetailQuizService::class.java)

    fun count(): Long {
        return detailQuizRepository.count()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long, actor: Member): DetailQuizWithHistoryDto {
        // 퀴즈 가져오기
        val quiz = detailQuizRepository.findById(id)
            .orElseThrow {
                ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: $id")
            }

        //dto로 변환
        val detailQuizResDto = DetailQuizResDto(quiz)
        val histories: List<QuizHistory> = quizHistoryRepository.findByMember(actor)

        // 필터링: 퀴즈 ID가 일치하고, 퀴즈 타입이 DETAIL인 히스토리만 추출
        val quizHistory = histories.firstOrNull { it.quizId == id && it.quizType == QuizType.DETAIL }

        // 퀴즈 히스토리가 없으면 null 반환
        return if (quizHistory == null) {
            DetailQuizWithHistoryDto(detailQuizResDto, null, false, 0, QuizType.DETAIL)
        } else {
            DetailQuizWithHistoryDto(
                detailQuizResDto,
                quizHistory.answer,
                quizHistory.isCorrect,
                quizHistory.gainExp,
                quizHistory.quizType
            )
        }
    }

    @Transactional(readOnly = true)
    fun findByNewsId(newsId: Long): List<DetailQuiz> {
        val news = realNewsRepository.findById(newsId)
            .orElseThrow {
                ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: $newsId")
            }

        val quizzes = detailQuizRepository.findByRealNewsId(newsId)

        if (quizzes.isEmpty()) {
            throw ServiceException(404, "해당 뉴스에 대한 상세 퀴즈가 존재하지 않습니다. newsId: $newsId")
        }

        return quizzes
    }


    // newsId로 뉴스 조회 후 AI api 호출해 퀴즈 생성
    fun generateQuizzes(newsId: Long): List<DetailQuizDto> {
        val news = realNewsRepository.findById(newsId)
            .orElseThrow {
                ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: $newsId")
            }

        val req = DetailQuizCreateReqDto(news.title, news.content)
        val processor = DetailQuizProcessor(req, objectMapper)

        return aiService.process(processor)
    }

    // 생성한 퀴즈 DB에 저장
    @Transactional
    fun saveQuizzes(newsId: Long, quizzes: List<DetailQuizDto>): List<DetailQuiz> {
        val news = realNewsRepository.findById(newsId)
            .orElseThrow {
                ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: $newsId")
            }

        detailQuizRepository.deleteByRealNewsId(newsId) // 기존 퀴즈 삭제
        news.detailQuizzes.clear()

        val savedQuizzes = quizzes.map { dto ->
            DetailQuiz(dto).apply { realNews = news } // RealNews 엔티티와 연관관계 설정
        }

        news.detailQuizzes.addAll(savedQuizzes)
        realNewsRepository.save(news) // RealNews 엔티티 저장 (CascadeType.ALL로 인해 DetailQuiz도 함께 저장됨)

        return savedQuizzes
    }


    @Transactional
    fun updateDetailQuiz(id: Long, detailQuizDto: DetailQuizDto): DetailQuiz {
        val quiz = detailQuizRepository.findById(id)
            .orElseThrow {
                ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: $id")
            }

        quiz.question = detailQuizDto.question
        quiz.option1 = detailQuizDto.option1
        quiz.option2 = detailQuizDto.option2
        quiz.option3 = detailQuizDto.option3
        quiz.correctOption = detailQuizDto.correctOption

        return detailQuizRepository.save(quiz)
    }

    @Transactional
    fun submitDetailQuizAnswer(actor: Member, id: Long, selectedOption: Option): DetailQuizAnswerDto {
        val quiz = detailQuizRepository.findById(id)
            .orElseThrow {
                ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: $id")
            }

        val managedActor = memberRepository.findById(actor.id)
            .orElseThrow {
                ServiceException(404, "회원이 존재하지 않습니다.")
            }

        val isCorrect = quiz.isCorrect(selectedOption)
        val gainExp = if (isCorrect) 10 else 0 // 정답 제출 시 경험치 10점 부여

        managedActor.apply {
            exp += gainExp
            level = calculateLevel(exp)
        }

        quizHistoryService.save(
            managedActor,
            id,
            quiz.quizType,
            selectedOption.toString(),
            isCorrect,
            gainExp
        ) // 퀴즈 히스토리 저장

        return DetailQuizAnswerDto(
            quiz.id,
            quiz.question,
            quiz.correctOption,
            selectedOption,
            isCorrect,
            gainExp,
            quiz.quizType
        )
    }
}
