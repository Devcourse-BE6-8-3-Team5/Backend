package com.back.domain.quiz.fact.eventListener;

import com.back.domain.news.fake.event.FakeNewsCreatedEvent;
import com.back.domain.quiz.fact.service.FactQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FactQuizEventListener {
    private final FactQuizService factQuizService;

    @EventListener
    public void handleFakeNewsCreated(FakeNewsCreatedEvent event) {
        List<Long> realNewsIds = event.getRealNewsIds();

        if (realNewsIds == null || realNewsIds.isEmpty()) {
            return; // 처리할 뉴스가 없으면 종료
        }

        try {
            factQuizService.create(realNewsIds);
        } catch (Exception e) {
            log.error("팩트 퀴즈 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
