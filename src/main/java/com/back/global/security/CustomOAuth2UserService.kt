package com.back.global.security

import com.back.domain.member.member.service.MemberService
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private enum class OAuth2Provider {
    KAKAO, GOOGLE, NAVER;

    companion object {
        fun from(registrationId: String): OAuth2Provider =
            entries.firstOrNull { it.name.equals(registrationId, ignoreCase = true) }
                ?: error("Unsupported provider: $registrationId")
    }
}

@Service
class CustomOAuth2UserService(
    private val memberService: MemberService
) : DefaultOAuth2UserService() {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val provider = OAuth2Provider.from(userRequest.clientRegistration.registrationId)

        val (oauthUserId, nickname) = when (provider) {
            OAuth2Provider.KAKAO -> {
                val props = oAuth2User.attributes["properties"] as Map<String, Any>
                Pair(
                    oAuth2User.name,
                    props["nickname"] as String
                )
            }
            OAuth2Provider.GOOGLE -> {
                val attrs = oAuth2User.attributes
                Pair(
                    oAuth2User.name,
                    attrs["name"] as String
                )
            }
            OAuth2Provider.NAVER -> {
                val resp = oAuth2User.attributes["response"] as Map<String, Any>
                Pair(
                    resp["id"] as String,
                    resp["nickname"] as String
                )
            }
        }

        val email = "$oauthUserId@${provider.name.lowercase()}.com"

        logger.debug("OAuth2 login success: provider={}, oauthUserId={}", provider.name, oauthUserId)
        logger.debug("Resolved email={}", email)

        val member = memberService.modifyOrJoin(oauthUserId, email, nickname).data!!


        logger.debug("Member upserted: id={}, email={}", member.id, member.email)

        return SecurityUser(
            member.id,
            member.email,
            member.name,
            member.password ?: "",
            member.authorities
        )
    }
}
