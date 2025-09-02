package com.back.domain.news.fake.repository

interface FakeNewsRepositoryCustom {
    fun findExistingIds(ids: List<Long>): List<Long>
}