package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;
import com.back.global.util.LevelSystem;
import lombok.Getter;


//경험치,레벨까지 포함한 DTO
@Getter
public class MemberWithInfoDto {
    private Long id;
    private String name;
    private String email;
    private int exp;
    private int level;
    private String role;
    private String characterImage; // 레벨에 따른 캐릭터 이미지

    public MemberWithInfoDto(Member member) {
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.exp = member.getExp();
        this.level = LevelSystem.calculateLevel(member.getExp());
        this.characterImage = LevelSystem.getImageByLevel(level);
        this.role = member.getRole();
    }
}
