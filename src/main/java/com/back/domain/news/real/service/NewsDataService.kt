package com.back.domain.news.real.service

import com.back.domain.news.common.dto.AnalyzedNewsDto
import com.back.domain.news.common.dto.NaverNewsDto
import com.back.domain.news.common.dto.NewsDetailDto
import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.entity.RealNews
import com.back.domain.news.real.mapper.RealNewsMapper
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.news.today.entity.TodayNews
import com.back.domain.news.today.event.TodayNewsCreatedEvent
import com.back.domain.news.today.repository.TodayNewsRepository
import com.back.global.exception.ServiceException
import com.back.global.rateLimiter.RateLimiter
import com.back.global.util.HtmlEntityDecoder
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openkoreantext.processor.OpenKoreanTextProcessorJava
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Set
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream

@Slf4j
@Service
@RequiredArgsConstructor
class NewsDataService {
    private val realNewsRepository: RealNewsRepository? = null
    private val todayNewsRepository: TodayNewsRepository? = null
    private val realNewsMapper: RealNewsMapper? = null
    private val objectMapper: ObjectMapper? = null
    private val rateLimiter: RateLimiter? = null
    private val publisher: ApplicationEventPublisher? = null

    // HTTP 요청을 보내기 위한 Spring의 HTTP 클라이언트(외부 API 호출 시 사용)
    private val restTemplate: RestTemplate? = null

    @Value("\${NAVER_CLIENT_ID}")
    private val clientId: String? = null

    @Value("\${NAVER_CLIENT_SECRET}")
    private val clientSecret: String? = null

    @Value("\${naver.news.display}")
    private val newsDisplayCount = 0

    @Value("\${naver.news.sort:sim}")
    private val newsSortOrder: String? = null

    @Value("\${naver.crawling.delay}")
    private val crawlingDelay = 0

    @Value("\${naver.base-url}")
    private val naverUrl: String? = null

    @Value("\${news.dedup.description.threshold}") // 요약본 임계값
    private val descriptionSimilarityThreshold = 0.0

    @Value("\${news.dedup.title.threshold}") // 제목 임계값
    private val titleSimilarityThreshold = 0.0

    // 서비스 초기화 시 설정값 검증
    @PostConstruct
    fun validateConfig() {
        require(!(clientId == null || clientId.isEmpty())) { "NAVER_CLIENT_ID가 설정되지 않았습니다." }
        require(!(clientSecret == null || clientSecret.isEmpty())) { "NAVER_CLIENT_SECRET가 설정되지 않았습니다." }
        require(!(newsDisplayCount < 1 || newsDisplayCount >= 100)) { "NAVER_NEWS_DISPLAY_COUNT는 100이하의 값이어야 합니다." }
        require(crawlingDelay >= 0) { "NAVER_CRAWLING_DELAY는 0 이상이어야 합니다." }
        require(!(naverUrl == null || naverUrl.isEmpty())) { "NAVER_BASE_URL이 설정되지 않았습니다." }
    }

    @Transactional
    fun createRealNewsDtoByCrawl(MetaDataList: MutableList<NaverNewsDto>): MutableList<RealNewsDto?> {
        val allRealNewsDtos: MutableList<RealNewsDto?> = ArrayList<RealNewsDto?>()

        try {
            for (metaData in MetaDataList) {
                val newsDetailData = crawladditionalInfo(metaData.link)

                if (newsDetailData.isEmpty()) {
                    // 크롤링 실패 시 해당 뉴스는 건너뜀
                    NewsDataService.log.warn("크롤링 실패: {}", metaData.link)
                    continue
                }

                val realNewsDto = makeRealNewsFromInfo(metaData, newsDetailData.get())
                NewsDataService.log.info("새 뉴스 생성 - ID: {}, 제목: {}", realNewsDto.id, realNewsDto.title)
                allRealNewsDtos.add(realNewsDto)

                Thread.sleep(crawlingDelay.toLong())
            }
            return allRealNewsDtos
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt() // 인터럽트 상태 복원
            NewsDataService.log.error("크롤링 중 인터럽트 발생", e)
            return mutableListOf<RealNewsDto?>()
        }
    }

    @Transactional
    fun saveAllRealNews(realNewsDtoList: MutableList<RealNewsDto?>): MutableList<RealNewsDto?> {
        // DTO → Entity 변환 후 저장
        val realNewsList: MutableList<RealNews?> = realNewsMapper!!.toEntityList(realNewsDtoList)
        val savedEntities = realNewsRepository!!.saveAll<RealNews?>(realNewsList) // 저장된 결과 받기

        // Entity → DTO 변환해서 반환
        return realNewsMapper.toDtoList(savedEntities)
    }

    // 네이버 API를 통해 메타데이터 수집
    fun collectMetaDataFromNaver(keywords: MutableList<String?>): MutableList<NaverNewsDto?> {
        val allNews: MutableList<NaverNewsDto?> = ArrayList<NaverNewsDto?>()
        NewsDataService.log.info("네이버 API 호출 시작: {} 개 키워드", keywords.size)

        val futures = keywords.stream()
            .map<CompletableFuture<MutableList<NaverNewsDto?>?>?> { keyword: String? -> this.fetchNews(keyword!!) }  // 비동기 처리
            .toList()

        try {
            CompletableFuture.allOf(*futures.toTypedArray<CompletableFuture<*>?>()).get()

            for (future in futures) {
                val news: MutableList<NaverNewsDto?> = future.get()!!

                val naverOnly = news.stream()
                    .filter { dto: NaverNewsDto? -> dto!!.link.contains("n.news.naver.com") }
                    .toList()

                allNews.addAll(naverOnly)
            }
        } catch (e: InterruptedException) {
            NewsDataService.log.error("뉴스 조회가 인터럽트됨", e)
        } catch (e: ExecutionException) {
            NewsDataService.log.error("뉴스 조회 중 오류 발생", e.cause)
        }

        return allNews
    }

    fun removeDuplicateByBitSetByField(
        metaDataList: MutableList<NaverNewsDto?>,
        fieldExtractor: Function<NaverNewsDto?, String?>,
        similarityThreshold: Double
    ): MutableList<NaverNewsDto?> {
        // 전체 키워드에 인덱스 부여 (존재하는 키워드에 대해 인덱스 부여)
        val keywordIndexMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()
        var idx = 0
        val newsKeywordSets: MutableList<MutableSet<String?>> = ArrayList<MutableSet<String?>>()

        for (news in metaDataList) {
            val keywords = extractKeywords(fieldExtractor.apply(news)!!)
            newsKeywordSets.add(keywords)
            for (kw in keywords) {
                if (!keywordIndexMap.containsKey(kw)) {
                    keywordIndexMap.put(kw, idx++)
                }
            }
        }

        // 뉴스 키워드  BitSet 변환(키워드의 인덱스에 대해 BitSet 설정)
        val newsBitSets: MutableList<BitSet?> = ArrayList<BitSet?>()
        for (keywords in newsKeywordSets) {
            val bs = BitSet(keywordIndexMap.size)
            for (kw in keywords) {
                bs.set(keywordIndexMap.get(kw)!!)
            }
            newsBitSets.add(bs)
        }

        // 3. BitSet 기반 유사도 비교 및 제거
        val filteredNews: MutableList<NaverNewsDto?> = ArrayList<NaverNewsDto?>()
        val removed = BooleanArray(metaDataList.size)

        for (i in newsBitSets.indices) {
            if (removed[i]) continue
            filteredNews.add(metaDataList.get(i))

            for (j in i + 1..<newsBitSets.size) {
                if (removed[j]) continue

                // 교집합
                val intersection = newsBitSets.get(i)!!.clone() as BitSet
                intersection.and(newsBitSets.get(j))
                val interCount = intersection.cardinality()

                // 합집합
                val union = newsBitSets.get(i)!!.clone() as BitSet
                union.or(newsBitSets.get(j))
                val unionCount = union.cardinality()

                val result = if (unionCount == 0) 0.0 else interCount.toDouble() / unionCount

                if (result > similarityThreshold) {
//                    log.info("\n\n{}\n{}\n유사도zzz: {}\n", fieldExtractor.apply(metaDataList.get(i)), fieldExtractor.apply(metaDataList.get(j)), result);
                    removed[j] = true
                }
            }
        }

        NewsDataService.log.info("중복 제거 전 : {}개, 중복 제거 후 : {}개", metaDataList.size, filteredNews.size)
        return filteredNews
    }


    fun extractKeywords(text: String): MutableSet<String?> {
        try {
            val keywords: MutableSet<String?> = HashSet<String?>()
            // OpenKoreanTextProcessor로 중복 체크
            val normalized = OpenKoreanTextProcessorJava.normalize(text).toString()
            val tokenList = OpenKoreanTextProcessorJava.tokensToJavaKoreanTokenList(
                OpenKoreanTextProcessorJava.tokenize(normalized)
            )

            for (token in tokenList) {
                val pos = token.getPos().toString()

                //System.out.printf("토큰: %s, 품사: %s\n", token.getText(), pos);

                // 조사, 어미, 구두점만 제외하고 나머지는 모두 포함
                if (!pos.contains("Josa") && !pos.contains("Eomi") && !pos.contains("Punctuation") && !pos.contains("Space")) {
                    if (pos == "Adjective" || pos == "Verb") {
                        // Adjective, Verb 이고 기본형이 있는 경우
                        val stem = token.getStem()
                        if (stem != null) {
                            //System.out.println("기본형: " + stem);
                            keywords.add(stem)
                            continue
                        }
                    }

                    keywords.add(token.getText())
                }
            }
            return keywords
        } catch (e: Exception) {
            // 형태소 분석 실패 시 단순 공백 기준 분리 (조사 포함)
            return Set.of<String?>(*text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        }
    }

    @Async("newsExecutor")
    fun fetchNews(keyword: String): CompletableFuture<MutableList<NaverNewsDto?>?> {
        try {
            rateLimiter!!.waitForRateLimit()

            val url = naverUrl + keyword + "&display=" + newsDisplayCount + "&sort=" + newsSortOrder

            // http 요청 헤더 설정 (아래는 네이버 디폴트 형식)
            val headers = HttpHeaders()
            headers.set("X-Naver-Client-Id", clientId)
            headers.set("X-Naver-Client-Secret", clientSecret)

            // http 요청 엔티티(헤더+바디) 생성
            // get이라 본문은 없고 헤더만 포함 -> 아래에서 string = null로 설정
            val entity = HttpEntity<String?>(headers)

            //http 요청 수행
            val response = restTemplate!!.exchange<String?>(
                url, HttpMethod.GET, entity, String::class.java, keyword
            )

            if (response.getStatusCode() === HttpStatus.OK) {
                // JsonNode: json 구조를 트리 형태로 표현. json의 중첩 구조를 탐색할 때 사용
                // readTree(): json 문자열을 JsonNode 트리로 변환
                val items = objectMapper!!.readTree(response.getBody()).get("items")

                if (items != null) {
                    val rawNews = getNewsMetaDataFromNaverApi(items)

                    // 네이버 뉴스만 필터링
                    val naverOnly = rawNews.stream()
                        .filter { dto: NaverNewsDto? -> dto!!.link.contains("n.news.naver.com") }
                        .toList()

                    // 키워드별로 중복 제거 수행
                    val dedupTitle = removeDuplicateByBitSetByField(
                        naverOnly, NaverNewsDto::title, titleSimilarityThreshold
                    )

                    val dedupDescription = removeDuplicateByBitSetByField(
                        dedupTitle, NaverNewsDto::description, descriptionSimilarityThreshold
                    )

                    // 12개로 제한
                    val limited = dedupDescription.stream()
                        .limit(12)
                        .toList()

                    NewsDataService.log.info(
                        "키워드 '{}': 원본 {}개 → 중복제거 후 {}개 → 제한 후 {}개",
                        keyword, naverOnly.size, dedupDescription.size, limited.size
                    )

                    return CompletableFuture.completedFuture<MutableList<NaverNewsDto?>?>(limited)
                }
                return CompletableFuture.completedFuture<MutableList<NaverNewsDto?>?>(ArrayList<NaverNewsDto?>())
            }
            throw ServiceException(500, "네이버 API 호출 실패: " + response.getStatusCode())
        } catch (e: JsonProcessingException) {
            throw ServiceException(500, "네이버 API 응답 파싱 실패")
        } catch (e: Exception) {
            throw RuntimeException("네이버 뉴스 조회 중 오류 발생", e)
        }
    }


    // 단건 크롤링
    fun crawladditionalInfo(naverNewsUrl: String): Optional<NewsDetailDto?> {
        try {
            val doc = Jsoup.connect(naverNewsUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)") // 브라우저인 척
                .get() // GET 요청으로 HTML 가져오기 (robots.txt에 걸리지 않도록)


            val content = Optional.ofNullable<Element?>(doc.selectFirst("article#dic_area"))
                .map<String>(Function { element: Element? -> this.extractTextWithLineBreaks(element!!) })
                .orElse("")

            val imgUrl = Optional.ofNullable<Element?>(doc.selectFirst("#img1"))
                .map<String>(Function { element: Element? -> element!!.attr("data-src") })
                .orElse("")

            val journalist = Optional.ofNullable<Element?>(doc.selectFirst("em.media_end_head_journalist_name"))
                .map<String>(Function { obj: Element? -> obj!!.text() })
                .orElse("")
            val mediaName = Optional.ofNullable<Element?>(doc.selectFirst("img.media_end_head_top_logo_img"))
                .map<String>(Function { elem: Element? -> elem!!.attr("alt") })
                .orElse("")

            // 크롤링한 정보가 비어있으면 null 반환
            if (content.isEmpty() || imgUrl.isEmpty() || journalist.isEmpty() || mediaName.isEmpty()) {
                return Optional.empty<NewsDetailDto?>()
            }

            return Optional.of<NewsDetailDto?>(NewsDetailDto(content, imgUrl, journalist, mediaName))
        } catch (e: IOException) {
            NewsDataService.log.warn("크롤링 실패: {}", naverNewsUrl)
            return Optional.empty<NewsDetailDto?>() // 예외 던지지 않고 빈 값 반환
        }
    }

    private fun extractTextWithLineBreaks(element: Element): String {
        element.select("p").before("\n\n")
        element.select("div").before("\n\n")
        element.select("br").before("\n")
        return element.text().replace("\\n", "\n")
    }

    // 네이버 api에서 받아온 정보와 크롤링한 상세 정보를 바탕으로 RealNewsDto 생성
    fun makeRealNewsFromInfo(naverNewsDto: NaverNewsDto, newsDetailDto: NewsDetailDto): RealNewsDto {
        return RealNewsDto(
            0L,
            naverNewsDto.title,
            newsDetailDto.content,
            naverNewsDto.description,
            naverNewsDto.link,
            newsDetailDto.imgUrl,
            parseNaverDate(naverNewsDto.pubDate)!!,
            LocalDateTime.now(),  // 생성일은 현재 시간으로 설정
            newsDetailDto.mediaName,
            newsDetailDto.journalist,
            naverNewsDto.originallink,
            NewsCategory.NOT_FILTERED
        )
    }

    // fetchNews 메서드로 네이버 API에서 뉴스 목록을 가져오고
    // 링크 정보를 바탕으로 상세 정보를 crawlAddtionalInfo 메서드로 크롤링하여 RealNews 객체를 생성
    private fun getNewsMetaDataFromNaverApi(items: JsonNode): MutableList<NaverNewsDto?> {
        val newsMetaDataList: MutableList<NaverNewsDto?> = ArrayList<NaverNewsDto?>()

        for (item in items) {
            val rawTitle = item.get("title").asText("")
            val originallink = item.get("originallink").asText("")
            val link = item.get("link").asText("")
            val rawDdscription = item.get("description").asText("")
            val pubDate = item.get("pubDate").asText("")

            val cleanedTitle = HtmlEntityDecoder.decode(rawTitle) // HTML 태그 제거
            val cleanDescription = HtmlEntityDecoder.decode(rawDdscription) // HTML 태그 제거

            //한 필드라도 비어있으면 건너뜀
            if (cleanedTitle.isEmpty() || originallink.isEmpty() || link.isEmpty() || cleanDescription.isEmpty() || pubDate.isEmpty()) continue
            //팩토리 메서드 사용
            val newsDto = NaverNewsDto(cleanedTitle, originallink, link, cleanDescription, pubDate)
            newsMetaDataList.add(newsDto)
        }

        return newsMetaDataList
    }

    // 네이버 API에서 받아온 날짜 문자열을 LocalDateTime으로 변환
    private fun parseNaverDate(naverDate: String?): LocalDateTime? {
        try {
            val cleaned = HtmlEntityDecoder.decode(naverDate)

            // 네이버 API 형식: "Tue, 29 Jul 2025 18:48:00 +0900"
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

            // ZonedDateTime으로 파싱 후 LocalDateTime으로 변환 (시간대 정보 제거)
            val zonedDateTime = ZonedDateTime.parse(cleaned, formatter)
            return zonedDateTime.toLocalDateTime()
        } catch (e: Exception) {
            NewsDataService.log.warn("날짜 파싱 실패: {}. 현재 시간으로 설정", naverDate)
            return LocalDateTime.now()
        }
    }

    @Transactional
    fun deleteRealNews(newsId: Long): Boolean {
        val realNewsOpt = realNewsRepository!!.findById(newsId)

        if (realNewsOpt.isEmpty()) {
            return false // 뉴스가 없으면 false 반환
        }

        if (todayNewsRepository!!.existsById(newsId)) {
            todayNewsRepository.deleteById(newsId)
        }
        // 뉴스 삭제 (FakeNews도 CASCADE로 함께 삭제됨)
        realNewsRepository.deleteById(newsId)
        return true
    }


    fun isAlreadyTodayNews(id: Long): Boolean {
        return todayNewsRepository!!.existsById(id)
    }

    @Transactional
    fun setTodayNews(id: Long) {
        val realNews = realNewsRepository!!.findById(id).orElseThrow<IllegalArgumentException?>
        (Supplier { IllegalArgumentException("해당 ID의 뉴스가 존재하지 않습니다. ID: " + id) })

        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        todayNewsRepository!!.deleteBySelectedDate(today)

        // 5. 새로운 오늘의 뉴스 생성
        val todayNews = TodayNews.builder()
            .selectedDate(today)
            .realNews(realNews)
            .build()

        todayNewsRepository.save<TodayNews?>(todayNews)

        publisher!!.publishEvent(TodayNewsCreatedEvent(todayNews.getId()))
    }

    fun count(): Int {
        return realNewsRepository!!.count().toInt()
    }

    fun selectNewsByScore(allRealNewsAfterFilter: MutableList<AnalyzedNewsDto?>): MutableList<RealNewsDto?> {
        return allRealNewsAfterFilter.stream()
            .collect(Collectors.groupingBy(AnalyzedNewsDto::category))
            .values
            .stream()
            .flatMap<AnalyzedNewsDto?> { categoryNews: MutableList<AnalyzedNewsDto?>? ->
                categoryNews!!.stream()
                    .sorted(Comparator.comparing<AnalyzedNewsDto?, Int?>(AnalyzedNewsDto::score).reversed())
                    .limit(4)
            }
            .map<RealNewsDto?>(AnalyzedNewsDto::realNewsDto)
            .toList()
    }

    fun addKeywords(keywords: MutableList<String?>, staticKeyword: MutableList<String?>): MutableList<String?> {
        return Stream.concat<String?>(keywords.stream(), staticKeyword.stream())
            .distinct()
            .toList()
    }

    @Transactional(readOnly = true)
    fun getAllRealNewsList(pageable: Pageable?): Page<RealNewsDto?> {
        return realNewsRepository!!.findAllByOrderByCreatedDateDesc(pageable)
            .map<RealNewsDto?>(Function { realNews: RealNews? -> realNewsMapper!!.toDto(realNews!!) })
    }
}
