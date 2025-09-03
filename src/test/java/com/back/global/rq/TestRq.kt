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
    private var _actor: Member? = null

    fun setActor(actor: Member) {
        _actor = actor
    }

    override val actor: Member?
        get() = _actor

    override fun getHeader(name: String, defaultValue: String): String = defaultValue
    override fun setHeader(name: String, value: String?) { /* do nothing */ }
    override fun getCookieValue(name: String, defaultValue: String): String = defaultValue
    override fun setCrossDomainCookie(name: String, value: String, maxAge: Int) { /* do nothing */ }
    override fun deleteCrossDomainCookie(name: String) { /* do nothing */ }
}
