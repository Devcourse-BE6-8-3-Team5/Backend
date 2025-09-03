package com.back.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.user.OAuth2User

class SecurityUser(
    val id: Long,
    val email: String,
    private val name: String,
    password: String,
    authorities: Collection<GrantedAuthority>
) : User(email, password, authorities), OAuth2User {

    override fun getAttributes(): Map<String, Any> = emptyMap()

    override fun getName(): String = name
}
