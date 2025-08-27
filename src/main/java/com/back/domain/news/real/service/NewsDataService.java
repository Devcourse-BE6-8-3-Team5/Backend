package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.dto.NewsDetailDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.mapper.RealNewsMapper;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.today.entity.TodayNews;
import com.back.domain.news.today.event.TodayNewsCreatedEvent;
import com.back.domain.news.today.repository.TodayNewsRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rateLimiter.RateLimiter;
import com.back.global.util.HtmlEntityDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openkoreantext.processor.KoreanTokenJava;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openkoreantext.processor.OpenKoreanTextProcessorJava.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsDataService {

    private final RealNewsRepository realNewsRepository;
    private final TodayNewsRepository todayNewsRepository;
    private final RealNewsMapper realNewsMapper;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    private final ApplicationEventPublisher publisher;

    // HTTP 요청을 보내기 위한 Spring의 HTTP 클라이언트(외부 API 호출 시 사용)
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

    @Value("${news.dedup.description.threshold}") // 요약본 임계값
    private double descriptionSimilarityThreshold;

    @Value("${news.dedup.title.threshold}") // 제목 임계값
    private double titleSimilarityThreshold;

    // 서비스 초기화 시 설정값 검증
    @PostConstruct
    public void validateConfig() {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("NAVER_CLIENT_ID가 설정되지 않았습니다.");
        }
        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new IllegalArgumentException("NAVER_CLIENT_SECRET가 설정되지 않았습니다.");
        }
        if (newsDisplayCount < 1 || newsDisplayCount >= 100) {
            throw new IllegalArgumentException("NAVER_NEWS_DISPLAY_COUNT는 100이하의 값이어야 합니다.");
        }
        if (crawlingDelay < 0) {
            throw new IllegalArgumentException("NAVER_CRAWLING_DELAY는 0 이상이어야 합니다.");
        }
        if (naverUrl == null || naverUrl.isEmpty()) {
            throw new IllegalArgumentException("NAVER_BASE_URL이 설정되지 않았습니다.");
        }
    }

    @Transactional
    public List<RealNewsDto> createRealNewsDtoByCrawl(List<NaverNewsDto> MetaDataList) {

        List<RealNewsDto> allRealNewsDtos = new ArrayList<>();

        try {
            for (NaverNewsDto metaData : MetaDataList) {

                Optional<NewsDetailDto> newsDetailData = crawladditionalInfo(metaData.link());

                if (newsDetailData.isEmpty()) {
                    // 크롤링 실패 시 해당 뉴스는 건너뜀
                    log.warn("크롤링 실패: {}", metaData.link());
                    continue;
                }

                RealNewsDto realNewsDto = makeRealNewsFromInfo(metaData, newsDetailData.get());
                log.info("새 뉴스 생성 - ID: {}, 제목: {}", realNewsDto.id(), realNewsDto.title());
                allRealNewsDtos.add(realNewsDto);

                Thread.sleep(crawlingDelay);
            }
            return allRealNewsDtos;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            log.error("크롤링 중 인터럽트 발생", e);
            return Collections.emptyList();
        }
    }

    @Transactional
    public List<RealNewsDto> saveAllRealNews(List<RealNewsDto> realNewsDtoList) {
        // DTO → Entity 변환 후 저장
        List<RealNews> realNewsList = realNewsMapper.toEntityList(realNewsDtoList);
        // 엔티티 변환 후 ID 확인
        for (RealNews entity : realNewsList) {
            log.debug("엔티티 변환 후 - ID: {}, 제목: {}", entity.getId(), entity.getTitle());
        }
        List<RealNews> savedEntities = realNewsRepository.saveAll(realNewsList); // 저장된 결과 받기

        for (RealNews saved : savedEntities) {
            log.info("저장 완료 -  제목: {}", saved.getTitle());
        }
        // Entity → DTO 변환해서 반환
        return realNewsMapper.toDtoList(savedEntities);
    }

    // 네이버 API를 통해 메타데이터 수집
    public List<NaverNewsDto> collectMetaDataFromNaver(List<String> keywords) {
        List<NaverNewsDto> allNews = new ArrayList<>();
        log.info("네이버 API 호출 시작: {} 개 키워드", keywords.size());

        List<CompletableFuture<List<NaverNewsDto>>> futures = keywords.stream()
                .map(this::fetchNews) // 비동기 처리
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

            for (CompletableFuture<List<NaverNewsDto>> future : futures) {
                List<NaverNewsDto> news = future.get();

                List<NaverNewsDto> naverOnly = news.stream()
                        .filter(dto -> dto.link().contains("n.news.naver.com"))
                        .toList();

                allNews.addAll(naverOnly);
            }

        } catch (InterruptedException e) {
            log.error("뉴스 조회가 인터럽트됨", e);
        } catch (ExecutionException e) {
            log.error("뉴스 조회 중 오류 발생", e.getCause());
        }

        return allNews;
    }

    public List<NaverNewsDto> removeDuplicateByBitSetByField(List<NaverNewsDto> metaDataList, Function<NaverNewsDto, String> fieldExtractor, double similarityThreshold) {
        // 전체 키워드에 인덱스 부여 (존재하는 키워드에 대해 인덱스 부여)
        Map<String, Integer> keywordIndexMap = new HashMap<>();
        int idx = 0;
        List<Set<String>> newsKeywordSets = new ArrayList<>();

        for (NaverNewsDto news : metaDataList) {
            Set<String> keywords = extractKeywords(fieldExtractor.apply(news));
            newsKeywordSets.add(keywords);
            for (String kw : keywords) {
                if (!keywordIndexMap.containsKey(kw)) {
                    keywordIndexMap.put(kw, idx++);
                }
            }
        }

        // 뉴스 키워드  BitSet 변환(키워드의 인덱스에 대해 BitSet 설정)
        List<BitSet> newsBitSets = new ArrayList<>();
        for (Set<String> keywords : newsKeywordSets) {
            BitSet bs = new BitSet(keywordIndexMap.size());
            for (String kw : keywords) {
                bs.set(keywordIndexMap.get(kw));
            }
            newsBitSets.add(bs);
        }

        // 3. BitSet 기반 유사도 비교 및 제거
        List<NaverNewsDto> filteredNews = new ArrayList<>();
        boolean[] removed = new boolean[metaDataList.size()];

        for (int i = 0; i < newsBitSets.size(); i++) {
            if (removed[i]) continue;
            filteredNews.add(metaDataList.get(i));

            for (int j = i + 1; j < newsBitSets.size(); j++) {
                if (removed[j]) continue;

                // 교집합
                BitSet intersection = (BitSet) newsBitSets.get(i).clone();
                intersection.and(newsBitSets.get(j));
                int interCount = intersection.cardinality();

                // 합집합
                BitSet union = (BitSet) newsBitSets.get(i).clone();
                union.or(newsBitSets.get(j));
                int unionCount = union.cardinality();

                double result = (unionCount == 0) ? 0.0 : (double) interCount / unionCount;

                if (result > similarityThreshold) {
//                    log.info("\n\n{}\n{}\n유사도zzz: {}\n", fieldExtractor.apply(metaDataList.get(i)), fieldExtractor.apply(metaDataList.get(j)), result);
                    removed[j] = true;
                }

            }
        }

        log.info("중복 제거 전 : {}개, 중복 제거 후 : {}개",metaDataList.size(), filteredNews.size());
        return filteredNews;
    }


    public Set<String> extractKeywords(String text) {
        try {
            Set<String> keywords = new HashSet<>();
            // OpenKoreanTextProcessor로 중복 체크
            String normalized = normalize(text).toString();
            List<KoreanTokenJava> tokenList = tokensToJavaKoreanTokenList(
                    tokenize(normalized)
            );

            for (KoreanTokenJava token : tokenList) {
                String pos = token.getPos().toString();
                //System.out.printf("토큰: %s, 품사: %s\n", token.getText(), pos);

                // 조사, 어미, 구두점만 제외하고 나머지는 모두 포함
                if (!pos.contains("Josa") && !pos.contains("Eomi") &&
                        !pos.contains("Punctuation") && !pos.contains("Space")) {

                    if(pos.equals("Adjective") || pos.equals("Verb")) {
                        // Adjective, Verb 이고 기본형이 있는 경우
                        String stem = token.getStem();
                        if (stem != null) {
                            //System.out.println("기본형: " + stem);
                            keywords.add(stem);
                            continue;
                        }
                    }

                    keywords.add(token.getText());
                }
            }
            return keywords;

        } catch (Exception e) {
            // 형태소 분석 실패 시 단순 공백 기준 분리 (조사 포함)
            return Set.of(text.split("\\s+"));
        }
    }

    @Async("newsExecutor")
    public CompletableFuture<List<NaverNewsDto>> fetchNews(String keyword) {
        try {
            rateLimiter.waitForRateLimit();

            String url = naverUrl + keyword + "&display=" + newsDisplayCount + "&sort=" + newsSortOrder;

            // http 요청 헤더 설정 (아래는 네이버 디폴트 형식)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            // http 요청 엔티티(헤더+바디) 생성
            // get이라 본문은 없고 헤더만 포함 -> 아래에서 string = null로 설정
            HttpEntity<String> entity = new HttpEntity<>(headers);

            //http 요청 수행
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class, keyword);

            if (response.getStatusCode() == HttpStatus.OK) {
                // JsonNode: json 구조를 트리 형태로 표현. json의 중첩 구조를 탐색할 때 사용
                // readTree(): json 문자열을 JsonNode 트리로 변환
                JsonNode items = objectMapper.readTree(response.getBody()).get("items");

                if (items != null) {
                    List<NaverNewsDto> rawNews = getNewsMetaDataFromNaverApi(items);

                    // 네이버 뉴스만 필터링
                    List<NaverNewsDto> naverOnly = rawNews.stream()
                            .filter(dto -> dto.link().contains("n.news.naver.com"))
                            .toList();

                    // 키워드별로 중복 제거 수행
                    List<NaverNewsDto> dedupTitle = removeDuplicateByBitSetByField(
                            naverOnly, NaverNewsDto::title, titleSimilarityThreshold);

                    List<NaverNewsDto> dedupDescription = removeDuplicateByBitSetByField(
                            dedupTitle, NaverNewsDto::description, descriptionSimilarityThreshold);

                    // 12개로 제한
                    List<NaverNewsDto> limited = dedupDescription.stream()
                            .limit(12)
                            .toList();

                    log.info("키워드 '{}': 원본 {}개 → 중복제거 후 {}개 → 제한 후 {}개",
                            keyword, naverOnly.size(), dedupDescription.size(), limited.size());

                    return CompletableFuture.completedFuture(limited);
                }
                return CompletableFuture.completedFuture(new ArrayList<>());
            }
            throw new ServiceException(500, "네이버 API 호출 실패: " + response.getStatusCode());

        } catch (JsonProcessingException e) {
            throw new ServiceException(500, "네이버 API 응답 파싱 실패");
        } catch (Exception e) {
            throw new RuntimeException("네이버 뉴스 조회 중 오류 발생", e);
        }

    }



    // 단건 크롤링
    public Optional<NewsDetailDto> crawladditionalInfo(String naverNewsUrl) {
        try {
            Document doc = Jsoup.connect(naverNewsUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")  // 브라우저인 척
                    .get();  // GET 요청으로 HTML 가져오기 (robots.txt에 걸리지 않도록)


            String content = Optional.ofNullable(doc.selectFirst("article#dic_area"))
                    .map(this::extractTextWithLineBreaks)
                    .orElse("");

            String imgUrl = Optional.ofNullable(doc.selectFirst("#img1"))
                    .map(element -> element.attr("data-src"))
                    .orElse("");

            String journalist = Optional.ofNullable(doc.selectFirst("em.media_end_head_journalist_name"))
                    .map(Element::text)
                    .orElse("");
            String mediaName = Optional.ofNullable(doc.selectFirst("img.media_end_head_top_logo_img"))
                    .map(elem -> elem.attr("alt"))
                    .orElse("");

            // 크롤링한 정보가 비어있으면 null 반환
            if (content.isEmpty() || imgUrl.isEmpty() || journalist.isEmpty() || mediaName.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(NewsDetailDto.of(content, imgUrl, journalist, mediaName));

        } catch (IOException e) {
            log.warn("크롤링 실패: {}", naverNewsUrl);
            return Optional.empty();  // 예외 던지지 않고 빈 값 반환
        }
    }

    private String extractTextWithLineBreaks(Element element) {
        element.select("p").before("\n\n");
        element.select("div").before("\n\n");
        element.select("br").before("\n");
        return element.text().replace("\\n", "\n");
    }

    // 네이버 api에서 받아온 정보와 크롤링한 상세 정보를 바탕으로 RealNewsDto 생성
    public RealNewsDto makeRealNewsFromInfo(NaverNewsDto naverNewsDto, NewsDetailDto newsDetailDto) {
        return RealNewsDto.of(
                null, // ID는 null로 시작, 저장 시 자동 생성
                naverNewsDto.title(),
                newsDetailDto.content(),
                naverNewsDto.description(),
                naverNewsDto.link(),
                newsDetailDto.imgUrl(),
                parseNaverDate(naverNewsDto.pubDate()),
                LocalDateTime.now(), // 생성일은 현재 시간으로 설정
                newsDetailDto.mediaName(),
                newsDetailDto.journalist(),
                naverNewsDto.originallink(),
                NewsCategory.NOT_FILTERED

        );
    }

    // fetchNews 메서드로 네이버 API에서 뉴스 목록을 가져오고
    // 링크 정보를 바탕으로 상세 정보를 crawlAddtionalInfo 메서드로 크롤링하여 RealNews 객체를 생성
    private List<NaverNewsDto> getNewsMetaDataFromNaverApi(JsonNode items) {
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
            if (cleanedTitle.isEmpty() || originallink.isEmpty() || link.isEmpty() || cleanDescription.isEmpty() || pubDate.isEmpty())
                continue;
            //팩토리 메서드 사용
            NaverNewsDto newsDto = NaverNewsDto.of(cleanedTitle, originallink, link, cleanDescription, pubDate);
            newsMetaDataList.add(newsDto);
        }

        return newsMetaDataList;
    }

    // 네이버 API에서 받아온 날짜 문자열을 LocalDateTime으로 변환
    private LocalDateTime parseNaverDate(String naverDate) {
        try {
            String cleaned = HtmlEntityDecoder.decode(naverDate);

            // 네이버 API 형식: "Tue, 29 Jul 2025 18:48:00 +0900"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

            // ZonedDateTime으로 파싱 후 LocalDateTime으로 변환 (시간대 정보 제거)
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(cleaned, formatter);
            return zonedDateTime.toLocalDateTime();

        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}. 현재 시간으로 설정", naverDate);
            return LocalDateTime.now();
        }
    }

    @Transactional
    public boolean deleteRealNews(Long newsId) {
        Optional<RealNews> realNewsOpt = realNewsRepository.findById(newsId);

        if (realNewsOpt.isEmpty()) {
            return false;  // 뉴스가 없으면 false 반환
        }

        if (todayNewsRepository.existsById(newsId)) {
            todayNewsRepository.deleteById(newsId);
        }
        // 뉴스 삭제 (FakeNews도 CASCADE로 함께 삭제됨)
        realNewsRepository.deleteById(newsId);
        return true;
    }


    public boolean isAlreadyTodayNews(Long id) {
        return todayNewsRepository.existsById(id);
    }

    @Transactional
    public void setTodayNews(Long id) {
        RealNews realNews = realNewsRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("해당 ID의 뉴스가 존재하지 않습니다. ID: " + id));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        todayNewsRepository.deleteBySelectedDate(today);

        // 5. 새로운 오늘의 뉴스 생성
        TodayNews todayNews = TodayNews.builder()
                .selectedDate(today)
                .realNews(realNews)
                .build();

        todayNewsRepository.save(todayNews);

        publisher.publishEvent(new TodayNewsCreatedEvent(todayNews.getId()));
    }

    public int count() {
        return (int) realNewsRepository.count();
    }

    public List<RealNewsDto> selectNewsByScore(List<AnalyzedNewsDto> allRealNewsAfterFilter) {
        return allRealNewsAfterFilter.stream()
                .collect(Collectors.groupingBy(AnalyzedNewsDto::category))
                .values()
                .stream()
                .flatMap(categoryNews ->
                        categoryNews.stream()
                                .sorted(Comparator.comparing(AnalyzedNewsDto::score).reversed())
                                .limit(4)
                )
                .map(AnalyzedNewsDto::realNewsDto)
                .toList();
    }

    public List<String> addKeywords(List<String> keywords, List<String> staticKeyword) {
        return Stream.concat(keywords.stream(), staticKeyword.stream())
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getAllRealNewsList(Pageable pageable) {
        return realNewsRepository.findAllByOrderByCreatedDateDesc(pageable)
                .map(realNewsMapper::toDto);
    }


}
