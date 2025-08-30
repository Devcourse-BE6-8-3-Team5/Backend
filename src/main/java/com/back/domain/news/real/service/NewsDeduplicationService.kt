package com.back.domain.news.real.service

import com.back.domain.news.common.dto.NaverNewsDto
import org.openkoreantext.processor.OpenKoreanTextProcessorJava.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class NewsDeduplicationService {

    companion object {
        private val log = LoggerFactory.getLogger(NewsDeduplicationService::class.java)
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
        }}}

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

            tokenList.forEach { token -> val pos = token.pos.toString()

                // 조사, 어미, 구두점 제외
                if (listOf("Josa", "Eomi", "Punctuation", "Space").none { pos.contains(it) }) {
                    when (pos) {
                        "Adjective", "Verb" -> token.stem?.let { keywords.add(it) } ?: keywords.add(token.text)
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
}