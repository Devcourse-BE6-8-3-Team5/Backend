package com.back.domain.member.member.service

import com.back.domain.member.member.entity.Member
import com.back.global.standard.util.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService(
    @Value("\${custom.jwt.secretKey}")
    private val jwtSecretKey: String,

    @Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds: Int
) {
    fun genAccessToken(member: Member): String {
        val id = member.id
        val email = member.email
        val name = member.name
        val role = member.role

        return Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            mapOf(
                "id" to id,
                "email" to email,
                "name" to name,
                "role" to role
            )
        )
    }

    fun payload(accessToken: String): Map<String, Any>? {
        val parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken) ?: return null

        val id = (parsedPayload["id"] as Number).toLong()
        val email = parsedPayload["email"] as String
        val name = parsedPayload["name"] as String
        val role = parsedPayload["role"] as String

        return mapOf(
            "id" to id,
            "email" to email,
            "name" to name,
            "role" to role
        )
    }
}
