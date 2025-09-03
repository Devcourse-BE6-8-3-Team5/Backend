package com.back.domain.member.member.repository

import com.back.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByApiKey(apiKey: String): Optional<Member>
    fun findByName(name: String): Optional<Member>
    fun findByOauthId(oauthId: String): Optional<Member>
    fun findTop5ByOrderByExpDesc(): MutableList<Member>
}