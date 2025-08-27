package com.back.domain.member.member.dto;


import com.back.domain.member.member.entity.Member;
import lombok.Getter;

@Getter
public class MemberWithRankDto {

    private String name; //이름
    private String email; //이메일
    private int exp; //경험치
    private int level; //레벨


    public MemberWithRankDto(Member member) {
        this.name = member.getName();
        this.email = member.getEmail();
        this.exp = member.getExp();
        this.level = member.getLevel();
    }
}


