package com.back.domain.member.member.controller

import com.back.domain.member.member.dto.MemberDto
import com.back.domain.member.member.dto.MemberWithAuthDto
import com.back.domain.member.member.dto.MemberWithInfoDto
import com.back.domain.member.member.dto.MemberWithRankDto
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/members")
@Tag(name = "MemberController", description = "회원 관련 컨트롤러 엔드 포인트")
class MemberController(
    private val memberService: MemberService,
    private val rq: Rq
) {

    @JvmRecord
    data class JoinReqBody(
        @field:NotBlank @field:Size(min = 2, max = 30, message = "이름은 최소 2자 이상이어야 합니다.") val name: String?,
        @field:NotBlank @field:Size(min = 10, max = 50) val password: String?,
        @field:NotBlank @field:Email(message = "유효한 이메일 형식이어야 합니다.") val email: String?
    )

    // 회원가입
    @PostMapping(value = ["/join"], produces = ["application/json;charset=UTF-8"])
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원 가입")
    fun join(@RequestBody @Valid reqBody: JoinReqBody): RsData<MemberDto> {
        memberService.findByEmail(reqBody.email)
            .ifPresent {
                throw ServiceException(409, "이미 존재하는 이메일입니다.")
            }

        // 회원 가입 진행
        val member = memberService.join(reqBody.name, reqBody.password, reqBody.email)

        return RsData(
            201,
            "${member.name}님 환영합니다. 회원 가입이 완료되었습니다.",
            MemberDto(member)
        )
    }


    // 로그인 요청 시 (이메일, 비밀번호)
    @JvmRecord
    data class LoginReqBody(
        @field:NotBlank @field:Email(message = "유효한 이메일 형식이어야 합니다.") val email: String?,
        @field:NotBlank val password: String?
    )

    // 로그인 응답 시 (MemberWithAuthDto, apiKey, accessToken)
    @JvmRecord
    data class LoginResBody(
        val member: MemberWithAuthDto?,
        val apiKey: String?,
        val accessToken: String?
    )

    // 로그인
    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "회원 로그인")
    fun login(@RequestBody @Valid reqBody: LoginReqBody): RsData<LoginResBody> {
        // 이메일로 회원 조회

        val member = memberService.findByEmail(reqBody.email).orElseThrow {
            ServiceException(401, "존재하지 않는 이메일입니다.")
        }

        // 비밀번호 검증
        if (!memberService.checkPassword(reqBody.password, member.password)) {
            throw ServiceException(401, "비밀번호가 일치하지 않습니다.")
        }

        // JWT 토큰 생성
        val accessToken = memberService.genAccessToken(member)

        // 쿠키 설정
        rq.setCrossDomainCookie("accessToken", accessToken, 60 * 20)
        rq.setCrossDomainCookie("apiKey", member.apiKey, 60 * 60 * 24 * 7)

        return RsData(
            200,
            "${member.name}님 환영합니다.",
            LoginResBody(
                MemberWithAuthDto(member),
                member.apiKey,
                accessToken
            )
        )
    }

    @Operation(summary = "회원 로그아웃")
    @DeleteMapping("/logout")
    fun logout(): RsData<Void> {
        rq.deleteCrossDomainCookie("accessToken")
        rq.deleteCrossDomainCookie("apiKey")

        return RsData(
            200,
            "로그아웃 성공",
            null
        )
    }

    @Operation(summary = "회원 정보 조회 = 마이페이지")
    @GetMapping("/info")
    @Transactional(readOnly = true)
    fun myInfo(): RsData<MemberWithInfoDto> {
        val actor = rq.actor
        if (actor == null) {
            throw ServiceException(401, "로그인이 필요합니다.")
        }

        val member = memberService.findById(actor.id)
            .orElseThrow { ServiceException(404, "존재하지 않는 회원입니다.") }

        return RsData(
            200,
            "내 정보 조회 완료",
            MemberWithInfoDto(member)
        )
    }

    @JvmRecord
    data class ModifyReqBody(
        @field:NotBlank @field:Size(min = 2, max = 30, message = "이름은 최소 2자 이상이어야 합니다.") val name: String?,
        @field:NotBlank @field:Size(min = 10, max = 50) val password: String?,
        @field:NotBlank @field:Email(message = "유효한 이메일 형식이어야 합니다.") val email: String?
    )

    @Operation(summary = "회원 정보 수정 (이름,비밀번호,메일)")
    @PutMapping("/info")
    @Transactional
    fun modifyInfo(@RequestBody @Valid reqBody: ModifyReqBody): RsData<MemberWithAuthDto> {
        val actor = rq.actor
        if (actor == null) {
            throw ServiceException(401, "로그인이 필요합니다.")
        }

        val member = memberService.findById(actor.id)
            .orElseThrow { ServiceException(404, "존재하지 않는 회원입니다.") }

        // 이메일 중복 체크
        if (member.email != reqBody.email) {
            memberService.findByEmail(reqBody.email)
                .ifPresent {
                    throw ServiceException(409, "이미 존재하는 이메일입니다.")
                }
        }

        memberService.modify(member, reqBody.name, reqBody.password, reqBody.email)

        return RsData(
            200,
            "회원 정보 수정 완료",
            MemberWithAuthDto(member)
        )
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/withdraw")
    @Transactional
    fun withdraw(): RsData<Void> {
        val actor = rq.actor
        if (actor == null) {
            throw ServiceException(401, "로그인이 필요합니다.")
        }
        val member = memberService.findById(actor.id)
            .orElseThrow { ServiceException(404, "존재하지 않는 회원입니다.") }

        memberService.withdraw(member)

        rq.deleteCrossDomainCookie("apiKey")
        rq.deleteCrossDomainCookie("accessToken")

        return RsData(
            200,
            "회원 탈퇴가 완료되었습니다.",
            null
        )
    }

    @Operation(summary = "회원 경험치순으로 5명까지 조회")
    @GetMapping("/rank")
    @Transactional(readOnly = true)
    fun rank(): RsData<MutableList<MemberWithRankDto>> {
        val members = memberService.getTop5MembersByExp()

        return RsData(
            200,
            "경험치 순위 조회 완료",
            members
        )
    }
}