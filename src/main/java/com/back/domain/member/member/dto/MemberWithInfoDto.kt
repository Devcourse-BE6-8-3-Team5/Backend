package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member
import com.back.global.util.LevelSystem.calculateLevel
import com.back.global.util.LevelSystem.getImageByLevel
import lombok.Getter

//경험치,레벨까지 포함한 DTO
@Getter
class MemberWithInfoDto(member: Member) {
    private val id: Long?
    private val name: String?
    private val email: String?
    private val exp: Int
    private val level: Int
    private val role: String?
    private val characterImage: String? // 레벨에 따른 캐릭터 이미지

    init {
        this.id = member.id
        this.name = member.name
        this.email = member.email
        this.exp = member.exp
        this.level = calculateLevel(member.exp)
        this.characterImage = getImageByLevel(level)
        this.role = member.role
    }
}
