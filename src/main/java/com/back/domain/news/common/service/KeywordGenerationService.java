package com.back.domain.news.common.service;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.common.dto.KeywordWithType;
import com.back.domain.news.common.enums.KeywordType;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.KeywordGeneratorProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordGenerationService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final KeywordHistoryService keywordHistoryService;
    private final KeywordCleanupService keywordCleanupService;
    @Value("${keyword.overuse.days}")
    private int overuseDays;

    @Value("${keyword.overuse.threshold}")
    private int overuseThreshold;

    @Value("${keyword.history.recent-days}")
    private int recentDays;
    /**
     * 오늘 날짜에 맞춰 키워드를 생성합니다.
     * - 최근 5일간 3회 이상 사용된 키워드 제외 (yml에서 overuseDays, overuseThreshold 설정값을 통해 조절)
     * - 어제 사용된 일반적인 키워드 제외
     *
     * @return 생성된 키워드 결과
     */
    public KeywordGenerationResDto generateTodaysKeywords() {
        LocalDate today = LocalDate.now();
        List<String> excludeKeywords = getExcludeKeywords();
        List<String> recentKeywords = keywordHistoryService.getRecentKeywords(recentDays);

        try{
            keywordCleanupService.cleanupKeywords();
        } catch(Exception e){
            log.warn("키워드 정리 중 오류 발생: {}", e.getMessage(), e);
        }
        KeywordGenerationReqDto keywordGenerationReqDto = new KeywordGenerationReqDto(today,recentKeywords, excludeKeywords);
        log.info("키워드 생성 요청 - 날짜 :  {} , 제외 키워드 : {}", today, excludeKeywords);

        try{

            KeywordGeneratorProcessor processor = new KeywordGeneratorProcessor(keywordGenerationReqDto, objectMapper);
            KeywordGenerationResDto result = aiService.process(processor);

            log.info("키워드 생성 결과 - {}", result);

            keywordHistoryService.saveKeywords(result, today);

            return result;
        } catch(Exception e) {
            log.error("키워드 생성 실패, 기본 키워드 사용: {}", e.getMessage());

            // 기본 키워드 사용 (processor의 createDefaultCase 로직 활용)
            KeywordGenerationResDto defaultResult = createDefaultCase();
            keywordHistoryService.saveKeywords(defaultResult, today);
            return defaultResult;
        }


    }

    private KeywordGenerationResDto createDefaultCase() {
        List<KeywordWithType> societyKeywords = List.of(
                new KeywordWithType("사회", KeywordType.GENERAL),
                new KeywordWithType("교육", KeywordType.GENERAL)
        );

        List<KeywordWithType> economyKeywords = List.of(
                new KeywordWithType("경제", KeywordType.GENERAL),
                new KeywordWithType("시장", KeywordType.GENERAL)
        );

        List<KeywordWithType> politicsKeywords = List.of(
                new KeywordWithType("정치", KeywordType.GENERAL),
                new KeywordWithType("정부", KeywordType.GENERAL)
        );

        List<KeywordWithType> cultureKeywords = List.of(
                new KeywordWithType("문화", KeywordType.GENERAL),
                new KeywordWithType("예술", KeywordType.GENERAL)
        );

        List<KeywordWithType> itKeywords = List.of(
                new KeywordWithType("기술", KeywordType.GENERAL),
                new KeywordWithType("IT", KeywordType.GENERAL)
        );

        return new KeywordGenerationResDto(
                societyKeywords,
                economyKeywords,
                politicsKeywords,
                cultureKeywords,
                itKeywords
        );
    }

    private List<String> getExcludeKeywords() {
        List<String> excludeKeywords = new ArrayList<>();

        // 1. 최근 5일간 3회 이상 사용된 키워드 (과도한 반복 방지)
        excludeKeywords.addAll(keywordHistoryService.getOverusedKeywords(overuseDays, overuseThreshold));
        // 2. 어제 사용된 키워드 중 일반적인 것들 (긴급 뉴스 제외)
        excludeKeywords.addAll(keywordHistoryService.getYesterdayKeywords());

        log.debug("제외 키워드 목록: {}", excludeKeywords);
        return excludeKeywords.stream().distinct().toList();
    }

}
