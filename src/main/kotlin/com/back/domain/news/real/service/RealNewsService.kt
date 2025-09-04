package com.back.domain.news.real.service

import com.back.domain.news.common.enums.NewsCategory
import com.back.domain.news.real.dto.RealNewsDto
import com.back.domain.news.real.entity.RealNews
import com.back.domain.news.real.mapper.RealNewsMapper
import com.back.domain.news.real.repository.RealNewsRepository
import com.back.domain.news.today.repository.TodayNewsRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RealNewsService(
    private val realNewsRepository: RealNewsRepository,
    private val realNewsMapper: RealNewsMapper,
    private val todayNewsRepository: TodayNewsRepository
) {

    @Transactional(readOnly = true)
    fun getRealNewsDtoById(id: Long): RealNewsDto? {
        return realNewsRepository.findById(id)
            .map { realNews -> realNewsMapper.toDto(realNews) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun searchRealNewsByTitle(title: String, pageable: Pageable): Page<RealNewsDto> {
        val excludedId = todayNewsOrRecent
        return realNewsRepository.findByTitleContainingAndIdNotOrderByCreatedDateDesc(title, excludedId, pageable)
            .map { realNews -> realNewsMapper.toDto(realNews) }
    }

    @Transactional(readOnly = true)
    fun getRealNewsListCreatedToday(): List<RealNewsDto> {
        val start = LocalDate.now().atStartOfDay()
        val end = LocalDateTime.now()

        return realNewsRepository.findByCreatedDateBetweenOrderByCreatedDateDesc(start, end)
            .map { realNews -> realNewsMapper.toDto(realNews) }
    }

    @Transactional(readOnly = true)
    fun getAllRealNewsByCategory(category: NewsCategory, pageable: Pageable): Page<RealNewsDto> {
        val excludedId = todayNewsOrRecent

        return realNewsRepository.findByNewsCategoryAndIdNotOrderByCreatedDateDesc(category, excludedId, pageable)
            .map { realNews -> realNewsMapper.toDto(realNews) }
    }

    @Transactional(readOnly = true)
    fun getRealNewsListExcludingNth(pageable: Pageable, n: Int): Page<RealNewsDto> {
        return executeExcludingNthQuery(pageable, n) { excludedId, rank, unsortedPageable ->
            realNewsRepository.findQAllExcludingNth(excludedId, rank, unsortedPageable)
        }
    }

    @Transactional(readOnly = true)
    fun searchRealNewsByTitleExcludingNth(title: String, pageable: Pageable, n: Int): Page<RealNewsDto> {
        return executeExcludingNthQuery(pageable, n) { excludedId, rank, unsortedPageable ->
            realNewsRepository.findQByTitleExcludingNthCategoryRank(title, excludedId, rank, unsortedPageable)
        }
    }

    @Transactional(readOnly = true)
    fun getRealNewsListByCategoryExcludingNth(category: NewsCategory, pageable: Pageable, n: Int): Page<RealNewsDto> {
        return executeExcludingNthQuery(pageable, n) { excludedId, rank, unsortedPageable ->
            realNewsRepository.findQByCategoryExcludingNth(category, excludedId, rank, unsortedPageable)
        }
    }

    private fun executeExcludingNthQuery(
        pageable: Pageable, n: Int,
        queryFunction: (Long, Int, Pageable) -> Page<RealNews>
    ): Page<RealNewsDto> {
        val excludedId = todayNewsOrRecent
        val unsortedPageable = createUnsortedPageable(pageable)
        
        return queryFunction(excludedId, n + 1, unsortedPageable)
            .map { realNews -> realNewsMapper.toDto(realNews) }
    }

    private fun createUnsortedPageable(pageable: Pageable): Pageable = 
        PageRequest.of(pageable.pageNumber, pageable.pageSize)

    @Transactional(readOnly = true)
    fun getAllRealNewsList(pageable: Pageable): Page<RealNewsDto> {
        return realNewsRepository.findAllByOrderByCreatedDateDesc(pageable)
            .map{ realNews: RealNews -> realNewsMapper.toDto(realNews) }
    }

    @get:Transactional(readOnly = true)
    val todayNewsOrRecent: Long
        get() = todayNewsRepository.findQTopByOrderBySelectedDateDescWithDetailQuizzes()
            ?.realNews?.id ?: -1L

}