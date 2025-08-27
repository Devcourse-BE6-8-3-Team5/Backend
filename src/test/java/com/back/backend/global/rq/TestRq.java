package com.back.backend.global.rq;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rq.Rq;

public class TestRq extends Rq {
    private Member actor;

    public TestRq() {
        // 실제 req, resp, service는 사용되지 않으므로 null 전달
        super(null, null, null);
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

