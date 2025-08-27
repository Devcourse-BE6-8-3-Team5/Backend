package com.back.global.ai.processor;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

@Slf4j
public class NewsAnalysisProcessor implements AiRequestProcessor<List<AnalyzedNewsDto>> {

    private final List<RealNewsDto> newsToAnalyze;
    private final ObjectMapper objectMapper;

    public NewsAnalysisProcessor(List<RealNewsDto> newsToAnalyze, ObjectMapper objectMapper) {
        if (newsToAnalyze == null || newsToAnalyze.isEmpty()) {
            throw new ServiceException(400, "분석할 뉴스가 제공되지 않았습니다");
        }

        this.newsToAnalyze = newsToAnalyze;
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildPrompt() {
        StringBuilder newsInput = new StringBuilder();

        for (int i = 0; i < newsToAnalyze.size(); i++) {
            RealNewsDto news = newsToAnalyze.get(i);
            newsInput.append("뉴스 ").append(i + 1).append(":\n\n")
                    .append("내용: ").append(cleanText(news.content()))
                    .append("\n---\n");
        }

        return String.format("""
                당신은 뉴스 분석 전문가입니다. 다음 %d개 뉴스를 모두 분석하여 JSON으로 응답하세요.
                
                === 반드시 수행해야 할 작업 ===
                
                1. **품질 평가** (0-100점)
                   - 화제성: 대중 관심을 끌 수 있는 소재인가? (25점)
                   - 완성도: 내용이 충실하고 흐름이 매끄러운가? (25점)
                   - 분량: 너무 짧지 않고 충분한 길이인가? (25점)
                   - 신뢰성: 사실 기반의 근거가 있는가? (25점)
                
                2. **카테고리 분류** (반드시 다음 중 하나 선택)
                   - POLITICS: 정부, 국회, 정책, 선거, 정치인 관련
                   - ECONOMY: 기업, 주식, 금리, GDP, 산업, 경제 관련
                   - SOCIETY: 사건사고, 복지, 교육, 환경, 사회 문제
                   - CULTURE: 문화, 예술, 스포츠, 연예, 엔터테인먼트
                   - IT: 기술, 인터넷, AI, 디지털, 컴퓨터 관련
                
                3. **본문 정제 작업**
                
                   **반드시 제거해야 할 내용:**
                   - 기자 이름 및 이메일 (예: "홍길동 기자", "reporter@news.com")
                   - 이미지 캡션 및 설명 (예: "[사진]", "이미지 출처", "사진=연합뉴스")
                   - 뉴스 발행일, 수정일 등 날짜 정보
                   - 본문과 무관한 홍보 문구, 링크, SNS 공유 안내
                   - 괄호로 처리된 메타정보 (예: "(서울=연합뉴스)", "(사진=EPA)")
                   - **본문 앞부분의 요약문/리드문** (예: "가계부채·환율은 부담…", "특검 내일 오전 9시 尹체포하러 간다…")
                   - **제목을 부연설명하는 서두 문장들**
                   - **상황 묘사문** (예: "31일 오전 서울역에서 뉴스가 나오고 있다")
                
                   **본문 시작 규칙:**
                   - 구체적인 사실이나 인용문부터 시작해야 함
                   - 육하원칙(5W1H)에 따른 구체적 내용부터 시작
                   - 올바른 예시: "김건희 여사 관련 의혹을 수사하는 민중기 특별검사팀이...", "대한상공회의소 등 경제6단체는 31일..."
                
                   **문단 구분 규칙:**
                   - 같은 주제의 연관된 문장들: 한 문단으로 묶어서 한 줄 개행(\\n) 또는 공백 없이 연결
                   - 문단이 바뀔 때만: 두 줄 개행(\\n\\n) 사용
                   - 문단 구분 기준:
                     * 시간이 바뀔 때 (어제 일 → 오늘 일)
                     * 화자가 바뀔 때 (정부 발표 → 전문가 의견)
                     * 주제가 전환될 때 (현황 설명 → 배경 설명 → 향후 전망)
                     * 인용문이 끝나고 새로운 내용이 시작될 때
                
                   **올바른 문단 구분 예시:**
                   "대한상공회의소 등 경제6단체는 31일 논평을 통해 환영 의사를 밝혔다. 이들은 '수출 환경 불확실성이 해소됐다'고 평가했다.\\n\\n앞서 한국과 미국은 상호관세율을 25%%에서 15%%로 인하하는 합의를 체결했다. 이는 일본, EU와 동일한 수준이다.\\n\\n경제6단체는 또한 국회의 신중한 검토를 요청했다."
                
                === JSON 작성 규칙 ===
                **매우 중요 - 다음 규칙을 절대 위반하지 마세요:**
                
                1. **JSON 구조**: 정확한 배열 형태로 작성
                2. **필수 필드**: newsIndex, qualityScore, category, cleanedContent 모두 포함
                3. **newsIndex**: 따옴표 없는 숫자 (1, 2, 3...)
                4. **qualityScore**: 따옴표 없는 숫자 (0-100)
                5. **category**: 따옴표로 감싼 문자열, 위 5개 중 정확히 하나
                6. **cleanedContent**: 따옴표로 감싼 문자열
                
                **cleanedContent 작성 시 이스케이프 규칙:**
                - 내부 따옴표: \\" (백슬래시 + 따옴표)
                - 작은따옴표: 그대로 ' 사용 (이스케이프 금지)
                - 개행 문자: \\n (백슬래시 + n)
                - 백슬래시: \\\\ (백슬래시 + 백슬래시)
                - 한글, 영문, 숫자: 그대로 사용 (유니코드 변환 금지)
                - 특수문자, 이모지: 그대로 사용 (이스케이프 금지)
                
                **금지 사항:**
                - JSON 외부에 다른 텍스트 추가 금지
                - 코드 블록(```) 사용 금지
                - 설명이나 주석 추가 금지
                
                === 최종 검증 체크리스트 ===
                응답 전 반드시 확인하세요:
                1. %d개 뉴스 모두 처리했는가?
                2. JSON 구조가 정확한가?
                3. 모든 필수 필드가 포함되었는가?
                4. category가 5개 중 하나인가?
                5. cleanedContent가 올바르게 이스케이프되었는가?
                6. 요약문/리드문이 제거되었는가?
                7. 문단 구분이 자연스러운가?
                
                === 개행 규칙 (절대 무시하지 말 것) ===
                **모든 뉴스에 대해 동일하게 적용:**
                - 관련 문장들: 붙여서 작성
                - 주제 바뀔 때: 반드시 \\n\\n 사용
                - 예시: "첫 문장. 두 번째 문장.\\n\\n새 주제 첫 문장."
                
                === 응답 형식 ===
                [
                  {
                    "newsIndex": 1,
                    "qualityScore": 85,
                    "category": "POLITICS",
                    "cleanedContent": "정제된 뉴스 본문 내용"
                  },
                  {
                    "newsIndex": 2,
                    "qualityScore": 72,
                    "category": "ECONOMY",
                    "cleanedContent": "정제된 뉴스 본문 내용"
                  }
                ]
                
                분석할 뉴스:
                %s
                """, newsToAnalyze.size(), newsToAnalyze.size(), newsInput);
    }


    @Override
    public List<AnalyzedNewsDto> parseResponse(ChatResponse response) {
        String text = response.getResult().getOutput().getText();
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답이 비어있습니다");
        }

        log.debug("AI 응답 길이: {}", text.length());

        try {
            String cleanedJson = cleanResponse(text);

            List<NewsAnalyzedRes> results = objectMapper.readValue(
                    cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NewsAnalyzedRes.class)
            );

            return convertToFilteredNewsDto(results);

        }  catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            throw new ServiceException(500, "AI 응답의 JSON 형식이 올바르지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("데이터 변환 실패: {}", e.getMessage());
            throw new ServiceException(500, "AI 응답 데이터 변환에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류: {}", e.getMessage());
            throw new ServiceException(500, "AI 응답 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
        }
    }

    //  AI 응답 정리 - 마크다운 코드 블록 제거
    private String cleanResponse(String text) {
        return text.trim()
                .replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1")
                .replaceAll("```", "")
                .trim();
    }

    //  프롬프트용 텍스트 정리
    private String cleanText(String text) {
        if (text == null) return "";
        return text.replace("\"", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    //  결과를 FilteredNewsDto로 변환
    private List<AnalyzedNewsDto> convertToFilteredNewsDto(List<NewsAnalyzedRes> results) {
        if (results.size() != newsToAnalyze.size()) {
            throw new ServiceException(500,
                    String.format("결과 개수 불일치: 요청 %d개, 응답 %d개", newsToAnalyze.size(), results.size()));
        }

        return results.stream().map(result -> {
            int index = result.newsIndex() - 1; // 1-based to 0-based
            RealNewsDto originalNews = newsToAnalyze.get(index);

            // 카테고리가 적용된 새로운 RealNewsDto 생성
            RealNewsDto updatedNews = RealNewsDto.of(
                    originalNews.id(),
                    originalNews.title(),
                    result.cleanedContent(),
                    originalNews.description(),
                    originalNews.link(),
                    originalNews.imgUrl(),
                    originalNews.originCreatedDate(),
                    originalNews.createdDate(),
                    originalNews.mediaName(),
                    originalNews.journalist(),
                    originalNews.originalNewsUrl(),
                    result.category()
            );

            return AnalyzedNewsDto.of(updatedNews, result.qualityScore(), result.category());
        }).toList();
    }

    // AI 응답 파싱용 내부 클래스
    private record NewsAnalyzedRes(
            @JsonProperty("newsIndex") int newsIndex,
            @JsonProperty("qualityScore") int qualityScore,
            @JsonProperty("category") NewsCategory category,
            @JsonProperty("cleanedContent") String cleanedContent
    ) {}
}