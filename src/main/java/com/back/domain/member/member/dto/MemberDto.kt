package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member

class MemberDto(
    val id: Long,
    val name: String,
    val email: String
) {
    constructor(member: Member): this(
        id = member.id,
        name = member.name,
        email = member.email
    )
}
