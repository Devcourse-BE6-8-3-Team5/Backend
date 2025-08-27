package com.back.global.ai.processor;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.common.dto.KeywordWithType;
import com.back.domain.news.common.enums.KeywordType;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

/**
 * 뉴스 제목과 본문을 기반 상세 퀴즈 3개를 생성하는 AI 요청 Processor 입니다.
 */
public class KeywordGeneratorProcessor implements AiRequestProcessor<KeywordGenerationResDto> {
    private final KeywordGenerationReqDto keywordGenerationReqDto;
    private final ObjectMapper objectMapper;

    public KeywordGeneratorProcessor(KeywordGenerationReqDto keywordGenerationReqDto, ObjectMapper objectMapper) {
        this.keywordGenerationReqDto = keywordGenerationReqDto;
        this.objectMapper = objectMapper;
    }

    // 뉴스 제목과 본문을 바탕으로 퀴즈 생성용 프롬프트 생성 (응답 형식을 JSON 형식으로 작성)
    @Override
    public String buildPrompt() {
        return String.format("""
                Task: 오늘 뉴스 수집을 위한 카테고리별 키워드를 생성하세요.
                
                
                ⚠️ 사전 조사 요청 - 키워드 생성 전에 다음 사항들을 웹 검색을 통해 먼저 파악해주세요:
                1. 네이버 뉴스 메인의 현재 주요 헤드라인 (최근 2-3시간 내)
                2. 구글 트렌드 한국 또는 다음/줌 등의 실시간 이슈 확인
                3. 각 카테고리별 최신 주요 이슈:
                - 정치: 국회/정부 최신 동향, 정책 발표
                - 경제: 증시/부동산/금융 정책 최신 소식
                - 사회: 최근 사건사고, 사회적 이슈
                - 문화: 연예/스포츠/엔터테인먼트 주요 뉴스
                - IT: 기술/게임/플랫폼 관련 최신 이슈
                4. 오늘 예정된 주요 일정이나 발표 (정부, 기업, 기관)
                
                추가 확인사항:
                - 최근 3일간 지속적으로 언급되는 키워드들
                - 주요 언론사(조선일보, 중앙일보 등) 메인 헤드라인
                - 국제 뉴스 중 국내에 영향을 줄 수 있는 이슈
                - 계절적/시기적 특성 (휴가철, 폭염, 장마 등)
                - SNS나 온라인 커뮤니티에서 화제가 되는 이슈
                
                ⚠️ 중요: 위 정보를 바탕으로 실제 현재 상황을 반영한 키워드를 생성해주세요.
                
                목적:
                - 네이버 뉴스 검색 API에서 효과적으로 검색되는 키워드 생성
                - 실제 뉴스 제목에 자주 사용되는 실용적인 키워드 선택
                - 각 키워드의 성격에 맞는 정확한 타입 분류

                ⚠️ 중요한 제약사항:
                - 키워드는 뉴스 제목에 실제로 포함될 수 있는 단어여야 합니다
                - 2-4글자의 단순하고 명확한 키워드 (예: "AI", "부동산", "선거")
                - 실제 기자들이 제목에 사용할 법한 키워드를 선택하세요

                현재 날짜: %s
                
                ⚠️ 실시간 상황 반영 지침:
                **웹 검색 결과를 바탕으로** 다음을 우선 고려:
                1. 현재 네이버 뉴스 메인 헤드라인의 핵심 키워드
                2. 구글 트렌드나 다른 플랫폼의 급상승 키워드
                3. 주요 언론사 메인 페이지의 공통 이슈
                4. 최근 7일간 지속적으로 보도되는 이슈
                5. 계절적 특성 및 오늘 예정된 주요 이벤트
                6. 국제적 이슈가 국내에 미치는 영향
                7. 새로운 정책 시행이나 제도 변화
                
                [실시간 트렌드 고려사항]
                - 정치: 현재 국회 일정, 선거 관련 이슈, 정책 발표
                - 경제: 금리 결정, 부동산 정책, 주요 기업 실적 발표
                - 사회: 사건사고, 날씨 이슈, 교육 정책
                - 문화: 새 영화/드라마 개봉, 스포츠 시즌, K팝 활동
                - IT: 새로운 기술 발표, 게임 출시, 보안 이슈
                
                [키워드 생성 및 타입 분류]
                - 각 카테고리당 정확히 2개의 키워드를 생성합니다
                - 각 키워드의 실제 특성을 분석하여 적절한 타입을 부여합니다
                - 시의성이 높은 키워드를 우선적으로 선택하세요
                
                [카테고리별 요구사항]
                - SOCIETY: 사회 이슈 관련 (최근 사건, 정책 변화, 사회 현상)
                - ECONOMY: 경제 관련 (시장 동향, 정책 발표, 기업 이슈)
                - POLITICS: 정치 관련 (국회 활동, 정책 논의, 선거 이슈)
                - CULTURE: 문화/생활 관련 (엔터테인먼트, 스포츠, 라이프스타일)
                - IT: IT/과학기술 (신기술, 게임, 보안, 플랫폼 이슈)
                
                [키워드 타입 분류 기준]
                - BREAKING: 최근 1-3일 내 급부상한 이슈, 긴급 사건, 속보성 키워드
                - ONGOING: 현재 진행 중인 지속적 관심사, 1-2주간 화제가 된 이슈
                - GENERAL: 평상시에도 꾸준히 다뤄지는 일반적 주제
                - SEASONAL: 현재 시기(계절/시점)와 강하게 연관된 키워드
                
                [기존 레포지토리 키워드 활용]
                다음은 최근 사용된 키워드들과 그 타입입니다: %s
                
                ⚠️ 레포지토리 키워드 재활용 규칙:
                1. ONGOING 타입 키워드: 여전히 진행 중이고 중요하다면 우선적으로 재선택
                2. BREAKING 타입 키워드: 3일 이내라면 계속 BREAKING, 그 이후는 ONGOING으로 전환 고려
                3. 기존 키워드가 새로운 전개나 이슈로 다시 주목받는다면 적극 포함
                4. 같은 키워드라도 상황 변화에 따라 타입 조정 가능
                
                [제외 키워드]
                다음 키워드들은 최근 과도하게 사용되어 제외해주세요: %s
                
                ⚠️ 예외: 제외 키워드라도 중대한 새로운 사건이나 완전히 다른 전개가 있다면 포함 가능합니다.
                
                [시기별 특성 반영]
                현재 날짜를 기준으로:
                - 7-8월: 여름휴가, 장마, 폭염, 올림픽/스포츠 시즌
                - 9-10월: 추석, 국정감사, 3분기 실적
                - 11-12월: 수능, 연말정산, 내년 예산
                - 1-2월: 설날, 새해 정책, 졸업/입학
                - 3-4월: 벚꽃, 새 학기, 1분기 실적
                - 5-6월: 어린이날, 어버이날, 상반기 결산
                
                📊 키워드 검증 체크리스트:
                1. **실시간 확인**: 현재 네이버 뉴스에서 실제 검색되는가?
                2. **제목 적합성**: 기자가 제목에 사용할 법한 단어인가?
                3. **현재 관심도**: 웹 검색 결과 기준 현재 화제성이 높은가?
                4. **형태 적합성**: 2-4글자의 간결한 형태인가?
                5. **카테고리 적합성**: 해당 카테고리에 적합한 키워드인가?
                6. **중복성 검토**: 기존 레포지토리나 제외 키워드와 겹치지 않는가?
                
                응답 형식:
                반드시 아래의 JSON 형식으로만 응답하세요. 설명 없이 JSON만 반환하세요.
                
                ```json
                {
                  "society": [
                    {"keyword": "교육", "keywordType": "GENERAL"},
                    {"keyword": "안전", "keywordType": "ONGOING"}
                  ],
                  "economy": [
                    {"keyword": "금리", "keywordType": "BREAKING"},
                    {"keyword": "부동산", "keywordType": "ONGOING"}
                  ],
                  "politics": [
                    {"keyword": "국회", "keywordType": "ONGOING"},
                    {"keyword": "선거", "keywordType": "SEASONAL"}
                  ],
                  "culture": [
                    {"keyword": "영화", "keywordType": "GENERAL"},
                    {"keyword": "축제", "keywordType": "SEASONAL"}
                  ],
                  "it": [
                    {"keyword": "AI", "keywordType": "BREAKING"},
                    {"keyword": "반도체", "keywordType": "ONGOING"}
                  ]
                }
                ```
                """,
                keywordGenerationReqDto.currentDate(),
                keywordGenerationReqDto.recentKeywordsWithTypes(),
                keywordGenerationReqDto.excludeKeywords());

    }

    // AI 응답을 파싱하여 KeywordGenerationResDto 리스트로 변환
    @Override
    public KeywordGenerationResDto parseResponse(ChatResponse response) {

        String text = response.getResult().getOutput().getText();
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답이 비어있습니다");
        }

        KeywordGenerationResDto result;
        String cleanedJson = text.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1").trim();

        try {
            result = objectMapper.readValue(
                    cleanedJson,
                    KeywordGenerationResDto.class
            );
        } catch (Exception e) {
            return createDefaultCase();
        }

        validatekeywords(result);

        return result;

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

    private void validatekeywords(KeywordGenerationResDto result) {
        if(result.society()== null || result.society().size() != 2 ||
           result.economy() == null || result.economy().size() != 2 ||
           result.politics() == null || result.politics().size() != 2 ||
           result.culture() == null || result.culture().size() != 2 ||
           result.it() == null || result.it().size() != 2) {
            throw new ServiceException(500, "각 카테고리당 정확히 2개의 키워드가 필요합니다.");
        }
    }
}
