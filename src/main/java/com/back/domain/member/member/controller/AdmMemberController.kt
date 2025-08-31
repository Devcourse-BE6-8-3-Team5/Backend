package com.back.domain.member.member.controller

import com.back.domain.member.member.dto.MemberWithInfoDto
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.RequiredArgsConstructor
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "AdmMemberController", description = "관리자 회원 단건, 다건 조회")
class AdmMemberController(
    private val memberService: MemberService
) {

    @Operation(summary = "(단건)회원 정보 조회- 관리자 전용 (아이디로 조회)")
    @GetMapping("/members/{id}")
    @Transactional
    fun getMemberById(@PathVariable id: Long): RsData<MemberWithInfoDto> {
        val member = memberService.findById(id)
            .orElseThrow{ ServiceException(404, "존재하지 않는 회원입니다.") }

        return RsData(
            200,
            "단건 회원 정보 조회 완료",
            MemberWithInfoDto(member)
        )
    }

    @Operation(summary = "(다건)전제 회원 정보 조회-관리자 전용")
    @GetMapping("/members")
    @Transactional(readOnly = true)
    fun listMembers(): RsData<MutableList<MemberWithInfoDto>> {
        val members = memberService.findAll()

        return RsData(
            200,
            "전체 회원 정보 조회 완료",
            members
                .map { MemberWithInfoDto(it) }
                .toMutableList()
        )
    }
}
