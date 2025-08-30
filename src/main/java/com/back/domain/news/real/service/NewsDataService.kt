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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openkoreantext.processor.OpenKoreanTextProcessorJava.*
import org.slf4j.LoggerFactory
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.stream.Stream

@Service
class NewsDataService(
    private val realNewsRepository: RealNewsRepository,
    private val todayNewsRepository: TodayNewsRepository,
    private val realNewsMapper: RealNewsMapper,
    private val objectMapper: ObjectMapper,
    private val rateLimiter: RateLimiter,
    private val publisher: ApplicationEventPublisher,
    private val restTemplate: RestTemplate
) {


    @Value("\${NAVER_CLIENT_ID}")
    private lateinit var clientId: String

    @Value("\${NAVER_CLIENT_SECRET}")
    private lateinit var clientSecret: String

    @Value("\${naver.news.display}")
    private var newsDisplayCount: Int = 0

    @Value("\${naver.news.sort:sim}")
    private lateinit var newsSortOrder: String

    @Value("\${naver.crawling.delay}")
    private var crawlingDelay: Int = 0

    @Value("\${naver.base-url}")
    private lateinit var naverUrl: String

    @Value("\${news.dedup.description.threshold}") // 요약본 임계값
    private var descriptionSimilarityThreshold: Double = 0.0

    @Value("\${news.dedup.title.threshold}") // 제목 임계값
    private var titleSimilarityThreshold: Double = 0.0

    companion object {
        private val log = LoggerFactory.getLogger(NewsDataService::class.java)
    }

    // 서비스 초기화 시 설정값 검증
    @PostConstruct
    fun validateConfig() {
        require(clientId.isNotBlank()) { "NAVER_CLIENT_ID가 설정되지 않았습니다." }
        require( clientSecret.isNotBlank()) { "NAVER_CLIENT_SECRET가 설정되지 않았습니다." }
        require(newsDisplayCount in 1..99) { "NAVER_NEWS_DISPLAY_COUNT는 100이하의 값이어야 합니다." }
        require(crawlingDelay >= 0) { "NAVER_CRAWLING_DELAY는 0 이상이어야 합니다." }
        require(naverUrl.isNotBlank()) { "NAVER_BASE_URL이 설정되지 않았습니다." }
    }

    @Transactional
    fun createRealNewsDtoByCrawl(metaDataList: List<NaverNewsDto>): List<RealNewsDto> {
        return runCatching {
            metaDataList.mapNotNull { metaData ->
                crawladditionalInfo(metaData.link)?.let { newsDetailData ->
                    makeRealNewsFromInfo(metaData, newsDetailData).also {
                        log.info("새 뉴스 생성: ${it.title}")
                    }}}
        }.onFailure { e ->
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
                log.error("크롤링 중 인터럽트 발생", e)
            }
        }.getOrElse { emptyList() }
    }

    @Transactional
    fun saveAllRealNews(realNewsDtoList: MutableList<RealNewsDto>): List<RealNewsDto> {
        // DTO → Entity 변환 후 저장
        val realNewsList: List<RealNews> = realNewsMapper.toEntityList(realNewsDtoList)
        val savedEntities = realNewsRepository.saveAll(realNewsList) // 저장된 결과 받기

        return realNewsMapper.toDtoList(savedEntities)
    }

    // 네이버 API를 통해 메타데이터 수집
    fun collectMetaDataFromNaver(keywords: MutableList<String>): List<NaverNewsDto> {
        log.info("네이버 API 호출 시작: {} 개 키워드", keywords.size)
        val futures = keywords.map { keyword -> fetchNews(keyword)}

        return runCatching {
            CompletableFuture.allOf(*futures.toTypedArray()).get()
            futures.flatMap { future -> (future.get() as List<NaverNewsDto>)
                .filter { dto -> dto.link.contains("n.news.naver.com") }
            }
        }.onFailure { e ->
            when (e) {
                is InterruptedException -> log.error("뉴스 조회가 인터럽트됨", e)
                is ExecutionException -> log.error("뉴스 조회 중 오류 발생", e.cause)
            }
        }.getOrElse { emptyList() }
    }

    fun removeDuplicateByBitSetByField(
        metaDataList: List<NaverNewsDto>,
        fieldExtractor: (NaverNewsDto) -> String,
        similarityThreshold: Double
    ): MutableList<NaverNewsDto> {
        // 전체 키워드에 인덱스 부여 (존재하는 키워드에 대해 인덱스 부여)
        val keywordIndexMap = mutableMapOf<String, Int>()
        var idx = 0
        val newsKeywordSets = metaDataList.map { news -> extractKeywords(fieldExtractor(news)).also {
            keywords -> keywords.forEach {
                kw -> if (kw !in keywordIndexMap)
                    keywordIndexMap[kw] = idx++
                }
            }
        }

        // 뉴스 키워드  BitSet 변환(키워드의 인덱스에 대해 BitSet 설정)
        val newsBitSets = newsKeywordSets.map { keywords ->
            BitSet(keywordIndexMap.size).apply {
                keywords.forEach { kw ->
                    set(keywordIndexMap[kw]!!)
                }
            }
        }

        // 3. BitSet 기반 유사도 비교 및 제거
        val filteredNews= mutableListOf<NaverNewsDto>()
        val removed = BooleanArray(metaDataList.size)

        newsBitSets.forEachIndexed { i, bitSetI ->
            if (removed[i]) return@forEachIndexed

            filteredNews.add(metaDataList[i])

            for (j in (i + 1) until newsBitSets.size) {
                if (removed[j]) continue

                // 교집합
                val intersection = bitSetI.clone() as BitSet
                intersection.and(newsBitSets[j])

                // 합집합
                val union = bitSetI.clone() as BitSet
                union.or(newsBitSets[j])

                val similarity = if (union.cardinality() == 0) 0.0
                else intersection.cardinality().toDouble() / union.cardinality()

                if (similarity > similarityThreshold) {
                    removed[j] = true
                }
            }
        }

        log.info("중복 제거 전: {}개, 후: {}개", metaDataList.size, filteredNews.size)
        return filteredNews
    }

    fun extractKeywords(text: String): Set<String> {
        return runCatching {
            val keywords = mutableSetOf<String>()
            val normalized = normalize(text).toString()
            val tokenList = tokensToJavaKoreanTokenList(
                tokenize(normalized)
            )

            tokenList.forEach { token ->
                val pos = token.pos.toString()

                // 조사, 어미, 구두점 제외
                if (listOf("Josa", "Eomi", "Punctuation", "Space").none { pos.contains(it) }) {
                    when (pos) {
                        "Adjective", "Verb" -> {
                            token.stem?.let { keywords.add(it) } ?: keywords.add(token.text)
                        }
                        else -> keywords.add(token.text)
                    }
                }
            }
            keywords.toSet()
        }.getOrElse {
            // 형태소 분석 실패 시 단순 공백 분리
            text.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        }
    }

    @Async("newsExecutor")
    fun fetchNews(keyword: String): CompletableFuture<List<NaverNewsDto>> {
        return runCatching {
            rateLimiter.waitForRateLimit()

            val url = "$naverUrl$keyword&display=$newsDisplayCount&sort=$newsSortOrder"

            val headers = HttpHeaders().apply {
                set("X-Naver-Client-Id", clientId)
                set("X-Naver-Client-Secret", clientSecret)
            }

            val entity = HttpEntity<String>(headers)
            val response = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)

            if (response.statusCode != HttpStatus.OK) {
                throw ServiceException(500, "네이버 API 호출 실패: ${response.statusCode}")
            }

            val items = objectMapper.readTree(response.body)?.get("items")
                ?: return@runCatching emptyList<NaverNewsDto>()

            val rawNews = getNewsMetaDataFromNaverApi(items)
            val naverOnly = rawNews.filter { it.link.contains("n.news.naver.com") }

            val dedupTitle = removeDuplicateByBitSetByField(naverOnly, { it.title }, titleSimilarityThreshold)
            val dedupDescription = removeDuplicateByBitSetByField(dedupTitle, { it.description }, descriptionSimilarityThreshold)
            val limited = dedupDescription.take(12)

            log.info("키워드 '${keyword}': 원본 ${naverOnly.size}개 → 중복제거 후 ${dedupDescription.size}개 → 제한 후 ${limited.size}개")

            limited
        }.fold(
            onSuccess = { CompletableFuture.completedFuture(it) },
            onFailure = { e ->
                when (e) {
                    is JsonProcessingException -> throw ServiceException(500, "네이버 API 응답 파싱 실패")
                    is ServiceException -> throw e
                    else -> throw RuntimeException("네이버 뉴스 조회 중 오류 발생", e)
                }
            }
        )
    }
    // 단건 크롤링
    fun crawladditionalInfo(naverNewsUrl: String): NewsDetailDto? {
        return runCatching {
            val doc = Jsoup.connect(naverNewsUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()

            val content = doc.selectFirst("article#dic_area")?.let { extractTextWithLineBreaks(it) } ?: ""
            val imgUrl = doc.selectFirst("#img1")?.attr("data-src") ?: ""
            val journalist = doc.selectFirst("em.media_end_head_journalist_name")?.text() ?: ""
            val mediaName = doc.selectFirst("img.media_end_head_top_logo_img")?.attr("alt") ?: ""

            // 모든 정보가 있는 경우만 반환
            if (listOf(content, imgUrl, journalist, mediaName).all { it.isNotBlank() }) {
                NewsDetailDto(content, imgUrl, journalist, mediaName)
            } else null
        }.onFailure { e ->
            if (e is IOException) {
                log.warn("크롤링 실패: {}", naverNewsUrl)
            }
        }.getOrNull()
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
            parseNaverDate(naverNewsDto.pubDate),
            LocalDateTime.now(),  // 생성일은 현재 시간으로 설정
            newsDetailDto.mediaName,
            newsDetailDto.journalist,
            naverNewsDto.originallink,
            NewsCategory.NOT_FILTERED
        )
    }

    // fetchNews 메서드로 네이버 API에서 뉴스 목록을 가져오고
    // 링크 정보를 바탕으로 상세 정보를 crawlAddtionalInfo 메서드로 크롤링하여 RealNews 객체를 생성
    private fun getNewsMetaDataFromNaverApi(items: JsonNode): List<NaverNewsDto> {

        return items.mapNotNull { item ->
            val rawTitle = item.get("title")?.asText("") ?: return@mapNotNull null
            val originallink = item.get("originallink")?.asText("") ?: return@mapNotNull null
            val link = item.get("link")?.asText("") ?: return@mapNotNull null
            val rawDescription = item.get("description")?.asText("") ?: return@mapNotNull null
            val pubDate = item.get("pubDate")?.asText("") ?: return@mapNotNull null

            val cleanedTitle = HtmlEntityDecoder.decode(rawTitle)
            val cleanDescription = HtmlEntityDecoder.decode(rawDescription)

            // 모든 필드가 비어있지 않은 경우만 DTO 생성
            if (listOf(cleanedTitle, originallink, link, cleanDescription, pubDate).all { it.isNotBlank() }) {
                NaverNewsDto(cleanedTitle, originallink, link, cleanDescription, pubDate)
            } else null
        }
    }

    // 네이버 API에서 받아온 날짜 문자열을 LocalDateTime으로 변환
    private fun parseNaverDate(naverDate: String): LocalDateTime {
        return runCatching {
            val cleaned = HtmlEntityDecoder.decode(naverDate)
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

            ZonedDateTime.parse(cleaned, formatter).toLocalDateTime()
        }.onFailure {
            log.warn("날짜 파싱 실패: {}. 현재 시간으로 설정", naverDate)
        }.getOrElse { LocalDateTime.now() }

    }

    @Transactional
    fun deleteRealNews(newsId: Long): Boolean {
        return realNewsRepository.findById(newsId).map { realNews ->
            if (todayNewsRepository.existsById(newsId))
                todayNewsRepository.deleteById(newsId)

            realNewsRepository.deleteById(newsId)
            true
        }.orElse(false)
    }

    fun isAlreadyTodayNews(id: Long): Boolean = todayNewsRepository.existsById(id)

    @Transactional
    fun setTodayNews(id: Long) {
        val realNews = realNewsRepository.findById(id).orElseThrow { IllegalArgumentException("해당 ID의 뉴스가 존재하지 않습니다. ID: $id") }

        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        todayNewsRepository.deleteBySelectedDate(today)

        val todayNews = TodayNews(
             selectedDate = today,
             realNews = realNews
        )

        val savedTodayNews = todayNewsRepository.save(todayNews)
        publisher.publishEvent(TodayNewsCreatedEvent(savedTodayNews.id))
    }

    fun count(): Int = realNewsRepository.count().toInt()

    fun selectNewsByScore(allRealNewsAfterFilter: List<AnalyzedNewsDto>): List<RealNewsDto> {
        return allRealNewsAfterFilter
            .groupBy { it.category }
            .values
            .flatMap { categoryNews -> categoryNews.sortedByDescending { it.score }.take(4) }
            .map { it.realNewsDto}
    }

    fun addKeywords(keywords: MutableList<String>, staticKeyword: MutableList<String>): MutableList<String> {
        return Stream.concat(keywords.stream(), staticKeyword.stream())
            .distinct()
            .toList()
    }

    @Transactional(readOnly = true)
    fun getAllRealNewsList(pageable: Pageable): Page<RealNewsDto> {
        return realNewsRepository.findAllByOrderByCreatedDateDesc(pageable)
            .map{ realNews: RealNews -> realNewsMapper.toDto(realNews) }
    }
}
