package com.back.domain.member.member.service

import com.back.domain.member.member.dto.MemberWithRankDto
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import lombok.RequiredArgsConstructor
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@RequiredArgsConstructor
class MemberService(
    private val memberRepository: MemberRepository,
    private val authTokenService: AuthTokenService,
    private val passwordEncoder: PasswordEncoder
) {

    // 기본 회원가입
    fun join(name: String, password: String, email: String): Member {
        val encodedPassword = passwordEncoder.encode(password)

        var role = "USER"
        if ("admin".equals(name, ignoreCase = true) || "system".equals(name, ignoreCase = true)) {
            role = "ADMIN" // 관리자 권한 설정
        }

        val member = Member(
            name,
            email,
            encodedPassword,
            0,
            1,
            role,
            UUID.randomUUID().toString(),
            null
        )

        return memberRepository.save(member)
    }

    // 소셜로그인으로 회원가입 & 회원 정보 수정
    fun modifyOrJoin(oauthId: String, email: String, nickname: String): RsData<Member?> {
        //oauthId로 기존 회원인지 확인

        var member = memberRepository.findByOauthId(oauthId).orElse(null)

        // 기존 회원이 아니면 소셜로그인으로 회원가입 진행
        if (member == null) {
            member = joinSocial(oauthId, email, nickname)
            return RsData(201, "회원가입이 완료되었습니다.", member)
        }

        // 기존 회원이면 회원 정보 수정
        modifySocial(member, nickname)
        return RsData(200, "회원 정보가 수정되었습니다.", member)
    }

    fun joinSocial(oauthId: String, email: String, nickname: String): Member {
        memberRepository.findByOauthId(oauthId)
            .ifPresent { throw ServiceException(409, "이미 존재하는 계정입니다.") }
        val encodedPassword = passwordEncoder.encode("1234")

        val member = Member(
            nickname,
            email,
            encodedPassword,
            0,
            1,
            "USER",
            UUID.randomUUID().toString(),
            oauthId
        )

        return memberRepository.save(member)
    }

    fun modifySocial(member: Member, nickname: String) {
        member.name = nickname
        memberRepository.save(member)
    }

    fun findByEmail(email: String): Optional<Member> {
        return memberRepository.findByEmail(email)
    }

    fun checkPassword(rawPassword: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }

    fun findById(id: Long): Optional<Member> {
        return memberRepository.findById(id)
    }

    fun payload(accessToken: String): Map<String, Any>? {
        return authTokenService.payload(accessToken)
    }


    fun findByApiKey(apiKey: String): Optional<Member> {
        return memberRepository.findByApiKey(apiKey)
    }

    fun genAccessToken(member: Member): String {
        return authTokenService.genAccessToken(member)
    }

    @Transactional
    fun modify(member: Member, name: String, password: String, email: String) {
        member.name = name
        member.password = passwordEncoder.encode(password)
        member.email = email
        memberRepository.save(member)
    }

    fun withdraw(member: Member) {
        if (member.isAdmin) throw ServiceException(403, "관리자는 탈퇴할 수 없습니다.")

        memberRepository.delete(member)
    }

    fun findAll(): MutableList<Member> {
        return memberRepository.findAll()
    }

    fun count(): Long {
        return memberRepository.count()
    }

    val top5MembersByExp: MutableList<MemberWithRankDto>
        get() {
            val members =
                memberRepository.findTop5ByOrderByExpDesc()

            return members
                .map { MemberWithRankDto(it) }
                .toMutableList()
        }
}
