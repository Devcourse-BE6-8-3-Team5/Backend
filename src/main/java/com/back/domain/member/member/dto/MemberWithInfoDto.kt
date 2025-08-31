package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member
import com.back.global.util.LevelSystem.calculateLevel
import com.back.global.util.LevelSystem.getImageByLevel

//경험치,레벨까지 포함한 DTO
class MemberWithInfoDto(
    val id: Long,
    val name: String,
    val email: String,
    val exp: Int,
    val level: Int,
    val role: String,
    val characterImage: String
) {

    constructor(member: Member): this(
        id = member.id,
        name = member.name,
        email = member.email,
        exp = member.exp,
        level = calculateLevel(member.exp),
        characterImage = getImageByLevel(calculateLevel(member.exp)),
        role = member.role
    )
}
