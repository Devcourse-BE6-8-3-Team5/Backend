package com.back.global.rq

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.mockito.Mockito.mock

class TestRq : Rq(
    mock(HttpServletRequest::class.java),
    mock(HttpServletResponse::class.java),
    mock(MemberService::class.java)
) {
    override var actor: Member? = null

    override fun getHeader(name: String, defaultValue: String): String {
        return defaultValue
    }

    override fun setHeader(name: String, value: String?) {}

    override fun getCookieValue(name: String, defaultValue: String): String {
        return defaultValue
    }

    override fun setCrossDomainCookie(name: String, value: String, maxAge: Int) {}

    override fun deleteCrossDomainCookie(name: String) {}
}

