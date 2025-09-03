package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member

class MemberWithAuthDto(
    val id: Long,
    val name: String,
    val email: String,
    val role: String
) {

    constructor(id: Long, email: String, name: String): this(
        id = id,
        name = name,
        email = email,
        role = "USER"
    )
    constructor(member: Member): this (
        id = member.id,
        name = member.name,
        email = member.email,
        role = member.role
    )
}


