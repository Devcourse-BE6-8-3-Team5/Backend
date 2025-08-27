package com.back.domain.news.fake.service;

import com.back.domain.news.common.service.KeepAliveMonitoringService;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.event.FakeNewsCreatedEvent;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.RealNewsService;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFakeNewsService {

    private final FakeNewsService fakeNewsService;
    private final RealNewsService realNewsService;
    private final ApplicationEventPublisher publisher;
    private final KeepAliveMonitoringService keepAliveMonitoringService;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul") // 매일 새벽 1시에 실행
    public void dailyFakeNewsProcess() {

        keepAliveMonitoringService.startBatchKeepAlive();

        try {
            List<RealNewsDto> realNewsDtos = realNewsService.getRealNewsListCreatedToday();

            if (realNewsDtos == null || realNewsDtos.isEmpty()) {
                log.warn("오늘 생성된 실제 뉴스가 없습니다.");
                return;
            }
            log.info("처리 대상 실제 뉴스: {}개", realNewsDtos.size());

            List<FakeNewsDto> fakeNewsDtos = fakeNewsService.generateAndSaveAllFakeNews(realNewsDtos);

            List<Long> successRealNewsIds = fakeNewsDtos.stream()
                    .map(FakeNewsDto::realNewsId)
                    .toList();

            publisher.publishEvent(new FakeNewsCreatedEvent(successRealNewsIds));

            log.info("=== 일일 가짜뉴스 생성 배치 완료 ===");
            log.info("요청: {}개, 성공: {}개, 실패: {}개",
                    realNewsDtos.size(),
                    fakeNewsDtos.size(),
                    realNewsDtos.size() - fakeNewsDtos.size());

        } catch (ServiceException e) {
            log.error("가짜 뉴스 생성 중 오류 발생: {}", e.getMessage());
        } finally {
            keepAliveMonitoringService.stopBatchKeepAlive();
        }
    }
}
