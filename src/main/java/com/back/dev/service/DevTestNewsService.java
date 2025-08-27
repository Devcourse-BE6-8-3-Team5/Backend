package com.back.dev.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.NewsAnalysisService;
import com.back.domain.news.real.service.NewsDataService;
import com.back.global.util.HtmlEntityDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openkoreantext.processor.KoreanTokenJava;
import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.util.*;
import java.util.stream.Collectors;

import static org.openkoreantext.processor.OpenKoreanTextProcessorJava.*;
import static org.openkoreantext.processor.tokenizer.KoreanTokenizer.*;
import static scala.collection.JavaConverters.*;

@Slf4j
@Profile("!prod")
@RequiredArgsConstructor
@Service
public class DevTestNewsService {
    private final NewsDataService newsDataService;
    private final RestTemplate restTemplate;

    @Value("${NAVER_CLIENT_ID}")
    private String clientId;

    @Value("${NAVER_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${naver.news.display}")
    private int newsDisplayCount;

    @Value("${naver.news.sort:sim}")
    private String newsSortOrder;

    @Value("${naver.crawling.delay}")
    private int crawlingDelay;

    @Value("${naver.base-url}")
    private String naverUrl;

    public List<NaverNewsDto> testNewsDataService() {
        List<String> newsKeywordsAfterAdd = List.of("AI", "사고");
        List<NaverNewsDto> newsAfterRemoveDup = newsDataService.collectMetaDataFromNaver(newsKeywordsAfterAdd);

//        List<RealNewsDto> newsAfterCrwal = newsDataService.createRealNewsDtoByCrawl(newsAfterRemoveDup);
//
//        List<AnalyzedNewsDto> newsAfterFilter = newsAnalysisService.filterAndScoreNews(newsAfterCrwal);
//
//        List<RealNewsDto> selectedNews = newsDataService.selectNewsByScore(newsAfterFilter);
//
//        List<RealNewsDto> savedNews = newsDataService.saveAllRealNews(selectedNews);

        return newsAfterRemoveDup;
    }

    public List<NaverNewsDto> fetchNews(String query) {

        try {
            //display는 한 번에 보여줄 뉴스의 개수, sort는 정렬 기준 (date: 최신순, sim: 정확도순)
            //일단 3건 패치하도록 해놨습니다. yml 에서 작성해서 사용하세요(10건 이상 x)
            String url = naverUrl + query + "&display=" + newsDisplayCount + "&sort=" + newsSortOrder;

            // http 요청 헤더 설정 (아래는 네이버 디폴트 형식)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            // http 요청 엔티티(헤더+바디) 생성
            // get이라 본문은 없고 헤더만 포함 -> 아래에서 string = null로 설정
            HttpEntity<String> entity = new HttpEntity<>(headers);

            //http 요청 수행
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class, query);

            if (response.getStatusCode() == HttpStatus.OK) {
                // JsonNode: json 구조를 트리 형태로 표현. json의 중첩 구조를 탐색할 때 사용
                // readTree(): json 문자열을 JsonNode 트리로 변환
                ObjectMapper mapper = new ObjectMapper();
                JsonNode items = mapper.readTree(response.getBody()).get("items");


                if (items != null) {
                    return getNewsMetaDataFromNaverApi(items);
                }
                return new ArrayList<>();
            }
            throw new RuntimeException("네이버 API 요청 실패");
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패");
        }
    }

    private List<NaverNewsDto> getNewsMetaDataFromNaverApi(JsonNode items){
        List<NaverNewsDto> newsMetaDataList = new ArrayList<>();

        for (JsonNode item : items) {
            String rawTitle = item.get("title").asText("");
            String originallink = item.get("originallink").asText("");
            String link = item.get("link").asText("");
            String rawDdscription = item.get("description").asText("");
            String pubDate = item.get("pubDate").asText("");

            String cleanedTitle = HtmlEntityDecoder.decode(rawTitle); // HTML 태그 제거
            String cleanDescription = HtmlEntityDecoder.decode(rawDdscription); // HTML 태그 제거

            //한 필드라도 비어있으면 건너뜀
            if(cleanedTitle.isEmpty()|| originallink.isEmpty() || link.isEmpty() || cleanDescription.isEmpty() || pubDate.isEmpty())
                continue;
            //팩토리 메서드 사용
            NaverNewsDto newsDto = NaverNewsDto.of(cleanedTitle, originallink, link, cleanDescription, pubDate);
            newsMetaDataList.add(newsDto);
        }

        return newsMetaDataList;
    }

}
