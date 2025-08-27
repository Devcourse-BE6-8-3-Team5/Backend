package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;
import lombok.Getter;


@Getter
public class MemberWithAuthDto {
    private Long id;
    private String name;
    private String email;
    private String role;


    public MemberWithAuthDto(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = "USER";
    }

    public MemberWithAuthDto(Member member) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.role = member.getRole();
    }
}


