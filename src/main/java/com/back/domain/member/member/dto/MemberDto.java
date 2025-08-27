package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;
import lombok.Getter;

@Getter
public class MemberDto {
    private Long id;
    private String name;
    private String email;

    public MemberDto(Member member) {
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
    }
}
