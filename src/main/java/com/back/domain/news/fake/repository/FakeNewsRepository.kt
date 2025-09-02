package com.back.domain.news.fake.repository

import com.back.domain.news.fake.entity.FakeNews
import org.springframework.data.jpa.repository.JpaRepository

interface FakeNewsRepository : JpaRepository<FakeNews, Long>, FakeNewsRepositoryCustom
