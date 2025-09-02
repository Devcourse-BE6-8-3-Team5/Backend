package com.back.global.rq;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rq.Rq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static org.mockito.Mockito.mock;

public class TestRq extends Rq {
    private Member actor;

    public TestRq() {
        // 기존 null에서 Mock 객체로 변경
        super(mock(HttpServletRequest.class), mock(HttpServletResponse.class), mock(MemberService.class));
    }

    public void setActor(Member actor) {
        this.actor = actor;
    }

    @Override
    public Member getActor() {
        return actor;
    }

    @Override
    public String getHeader(String name, String defaultValue) { return defaultValue; }
    @Override
    public void setHeader(String name, String value) { }
    @Override
    public String getCookieValue(String name, String defaultValue) { return defaultValue; }
    @Override
    public void setCrossDomainCookie(String name, String value, int maxAge) { }
    @Override
    public void deleteCrossDomainCookie(String name) { }
}

