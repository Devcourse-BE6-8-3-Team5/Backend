package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member


class MemberWithRankDto(
    val name: String,
    val email: String,
    val exp: Int,
    val level: Int
) {
    constructor(member: Member) : this (
        name = member.name,
        email = member.email,
        exp = member.exp,
        level = member.level
    )
}


