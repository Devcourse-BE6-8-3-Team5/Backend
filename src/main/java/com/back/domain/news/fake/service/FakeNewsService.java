package com.back.domain.news.fake.service;

import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.fake.repository.FakeNewsRepository;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.FakeNewsGeneratorProcessor;
import com.back.global.rateLimiter.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.*;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FakeNewsService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final FakeNewsRepository fakeNewsRepository;
    private final RealNewsRepository realNewsRepository;
    private final RateLimiter rateLimiter;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    @Qualifier("newsExecutor")
    private Executor executor;

    @Async("newsExecutor")
    public CompletableFuture<List<FakeNewsDto>> generateFakeNewsBatch(List<RealNewsDto> realNewsDtos) {
        if (realNewsDtos == null || realNewsDtos.isEmpty()) {
            log.warn("생성할 가짜뉴스가 없습니다.");
            return completedFuture(Collections.emptyList());
        }

        log.info("가짜뉴스 배치 생성 시작 (비동기) - 총 {}개", realNewsDtos.size());

        // 모든 뉴스를 비동기로 처리
        List<CompletableFuture<FakeNewsDto>> futures = realNewsDtos.stream()
                .map(realNewsDto -> supplyAsync(() -> {
                    try {
                        rateLimiter.waitForRateLimit(); // Rate limiting은 여기서
                        log.debug("가짜뉴스 생성 시작 - 실제뉴스 ID: {}", realNewsDto.id());

                        FakeNewsGeneratorProcessor processor = new FakeNewsGeneratorProcessor(realNewsDto, objectMapper);
                        FakeNewsDto result = aiService.process(processor);

                        log.debug("가짜뉴스 생성 완료 - 실제뉴스 ID: {}", realNewsDto.id());
                        return result;

                    } catch (Exception e) {
                        log.error("가짜뉴스 생성 실패 - 실제뉴스 ID: {}", realNewsDto.id(), e);
                        return null;
                    }
                }, executor)) // ← executor 사용
                .toList();

        // null 아닌 성공 결과 수집
        List<FakeNewsDto> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();;

        return completedFuture(results);
    }

    @Transactional
    public List<FakeNewsDto> generateAndSaveAllFakeNews(List<RealNewsDto> realNewsDtos){
        try{
            List<FakeNewsDto> fakeNewsDtos = generateFakeNewsBatch(realNewsDtos).get();

            if (fakeNewsDtos.isEmpty()) {
                log.warn("생성된 가짜뉴스가 없습니다.");
                return Collections.emptyList();
            }

            saveFakeNewsForBatch(fakeNewsDtos);
            return fakeNewsDtos;

        } catch (Exception e){
            log.error("가짜 뉴스 생성 및 저장 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void saveAllFakeNews(List<FakeNewsDto> fakeNewsDtos) {
        List<Long> realNewsIds = fakeNewsDtos.stream()
                .map(FakeNewsDto::realNewsId)
                .collect(Collectors.toList());

        // RealNews들을 한 번에 조회
        Map<Long, RealNews> realNewsMap = realNewsRepository.findAllById(realNewsIds)
                .stream()
                .collect(Collectors.toMap(RealNews::getId, Function.identity()));

        // FakeNews 엔티티들 생성 후 저장
        List<FakeNews> fakeNewsList = fakeNewsDtos.stream()
                .filter(dto -> realNewsMap.containsKey(dto.realNewsId())) // 존재하는 realNewsId만 필터링
                .map(dto -> FakeNews.builder()
                        .realNews(realNewsMap.get(dto.realNewsId()))
                        .content(dto.content())
                        .build())
                .collect(Collectors.toList());

        fakeNewsRepository.saveAll(fakeNewsList);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFakeNewsForBatch(List<FakeNewsDto> fakeNewsDtos) {
        try {
            log.info("=== FakeNews 배치 저장 시작 - 입력: {}개 ===",
                    fakeNewsDtos != null ? fakeNewsDtos.size() : 0);

            if (fakeNewsDtos == null || fakeNewsDtos.isEmpty()) {
                log.warn("저장할 FakeNewsDto가 없습니다.");
                return;
            }

            int savedCount = 0;
            int skipCount = 0;
            int errorCount = 0;

            for (FakeNewsDto dto : fakeNewsDtos) {
                try {
                    log.debug("처리 중: realNewsId={}", dto.realNewsId());

                    // 1. 이미 존재하는지 확인
                    if (fakeNewsRepository.existsById(dto.realNewsId())) {
                        log.debug("FakeNews 이미 존재 - ID: {}", dto.realNewsId());
                        skipCount++;
                        continue;
                    }

                    // 2. RealNews 프록시 객체 생성 (실제 DB 조회 없음, Lazy Loading)
                    RealNews realNewsProxy = realNewsRepository.getReferenceById(dto.realNewsId());

                    // 3. FakeNews 생성 및 저장
                    FakeNews fakeNews = FakeNews.builder()
                            .id(dto.realNewsId()) // 명시적 ID 설정 (필수!)
                            .realNews(realNewsProxy) // 프록시 객체 사용
                            .content(dto.content())
                            .build();
                    entityManager.persist(fakeNews); // merge()가 아닌 persist() 강제

                    savedCount++;
                    log.info("FakeNews 저장 성공 - ID: {}", dto.realNewsId());

                } catch (DataIntegrityViolationException e) {
                    // 동시성으로 인한 중복 키 에러
                    log.debug("FakeNews 중복 저장 시도 - ID: {} (동시성 이슈)", dto.realNewsId());
                    skipCount++;
                } catch (EntityNotFoundException e) {
                    // RealNews가 존재하지 않 는 경우
                    log.warn("RealNews 없음 - ID: {}", dto.realNewsId());
                    skipCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("FakeNews 저장 실패 - ID: {}, 원인: {}", dto.realNewsId(), e.getMessage(), e);
                }
            }

            log.info("=== FakeNews 배치 저장 완료 - 성공: {}개, 스킵: {}개, 실패: {}개 ===",
                    savedCount, skipCount, errorCount);

        } catch (Exception e) {
            log.error("FakeNews 배치 저장 중 전체 오류", e);
        }
    }

    public int count() {
        return (int) fakeNewsRepository.count();
    }
}

