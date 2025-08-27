package com.back.domain.quiz.detail.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository;
import com.back.domain.member.quizhistory.service.QuizHistoryService;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.detail.dto.*;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;
import com.back.domain.quiz.detail.repository.DetailQuizRepository;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.DetailQuizProcessor;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.back.global.util.LevelSystem.calculateLevel;

@Service
@RequiredArgsConstructor
@Slf4j // 로그 확인용
public class DetailQuizService {
    private final DetailQuizRepository detailQuizRepository;
    private final RealNewsRepository realNewsRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final QuizHistoryService quizHistoryService;
    private final QuizHistoryRepository quizHistoryRepository;
    private final MemberRepository memberRepository;

    public long count() {
        return detailQuizRepository.count();
    }

    @Transactional(readOnly = true)
    public DetailQuizWithHistoryDto findById(Long id, Member actor) {
        // 퀴즈 가져오기
        DetailQuiz quiz = detailQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: " + id));

        //dto로 변환
        DetailQuizResDto detailQuizResDto = new DetailQuizResDto(quiz);

        List<QuizHistory> histories = quizHistoryRepository.findByMember(actor);

        // 필터링: 퀴즈 ID가 일치하고, 퀴즈 타입이 DETAIL인 히스토리만 추출
        QuizHistory quizHistory = histories.stream()
                .filter(h -> h.getQuizId().equals(id) && h.getQuizType() == QuizType.DETAIL)
                .findFirst()
                .orElse(null);

        // 퀴즈 히스토리가 없으면 null 반환
        if (quizHistory == null) {
            return new DetailQuizWithHistoryDto(
                    detailQuizResDto,
                    null,
                    false,
                    0,
                    QuizType.DETAIL
            );
        }


        return new DetailQuizWithHistoryDto(
                detailQuizResDto,
                quizHistory.getAnswer(),
                quizHistory.isCorrect(),
                quizHistory.getGainExp(),
                quizHistory.getQuizType()
        );

    }

    @Transactional(readOnly = true)
    public List<DetailQuiz> findByNewsId(Long newsId) {
        RealNews news = realNewsRepository.findById(newsId)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: " + newsId));

        List<DetailQuiz> quizzes = detailQuizRepository.findByRealNewsId(newsId);

        if (quizzes.isEmpty()) {
            throw new ServiceException(404, "해당 뉴스에 대한 상세 퀴즈가 존재하지 않습니다. newsId: " + newsId);
        }

        return quizzes;
    }


    // newsId로 뉴스 조회 후 AI api 호출해 퀴즈 생성
    public List<DetailQuizDto> generateQuizzes(Long newsId) {
        RealNews news = realNewsRepository.findById(newsId)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: " + newsId));

        DetailQuizCreateReqDto req = new DetailQuizCreateReqDto(
                news.getTitle(),
                news.getContent()
        );

        DetailQuizProcessor processor = new DetailQuizProcessor(req, objectMapper);

        return aiService.process(processor);
    }

    // 생성한 퀴즈 DB에 저장
    @Transactional
    public List<DetailQuiz> saveQuizzes(Long newsId, List<DetailQuizDto> quizzes) {
        RealNews news = realNewsRepository.findById(newsId)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: " + newsId));

        detailQuizRepository.deleteByRealNewsId(newsId); // 기존 퀴즈 삭제

        List<DetailQuiz> savedQuizzes = quizzes.stream()
                .map(dto -> {
                    DetailQuiz quiz = new DetailQuiz(dto);
                    quiz.setRealNews(news); // RealNews 엔티티와 연관관계 설정
                    return quiz;
                })
                .toList();

        news.getDetailQuizzes().addAll(savedQuizzes);
        realNewsRepository.save(news); // RealNews 엔티티 저장 (CascadeType.ALL로 인해 DetailQuiz도 함께 저장됨)

        return savedQuizzes;
    }


    @Transactional
    public DetailQuiz updateDetailQuiz(Long id, DetailQuizDto detailQuizDto) {
        DetailQuiz quiz = detailQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: " + id));

        quiz.setQuestion(detailQuizDto.question());
        quiz.setOption1(detailQuizDto.option1());
        quiz.setOption2(detailQuizDto.option2());
        quiz.setOption3(detailQuizDto.option3());
        quiz.setCorrectOption(detailQuizDto.correctOption());

        return detailQuizRepository.save(quiz);
    }

    @Transactional
    public DetailQuizAnswerDto submitDetailQuizAnswer(Member actor, Long id, Option selectedOption) {

        DetailQuiz quiz = detailQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: " + id));

        Member managedActor = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException(404, "회원이 존재하지 않습니다."));

        boolean isCorrect = quiz.isCorrect(selectedOption);

        int gainExp = isCorrect ? 10 : 0; // 정답 제출 시 경험치 10점 부여


        managedActor.setExp(managedActor.getExp() + gainExp);

        managedActor.setLevel(calculateLevel(managedActor.getExp())); // 레벨 계산 로직 추가

        quizHistoryService.save(managedActor, id, quiz.getQuizType(), String.valueOf(selectedOption), isCorrect, gainExp); // 퀴즈 히스토리 저장

        return new DetailQuizAnswerDto(
                quiz.getId(),
                quiz.getQuestion(),
                quiz.getCorrectOption(),
                selectedOption,
                isCorrect,
                gainExp,
                quiz.getQuizType()
        );
    }
}
