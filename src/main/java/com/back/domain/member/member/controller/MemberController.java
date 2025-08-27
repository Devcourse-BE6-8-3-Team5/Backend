package com.back.domain.member.member.controller;

import com.back.domain.member.member.dto.MemberDto;
import com.back.domain.member.member.dto.MemberWithAuthDto;
import com.back.domain.member.member.dto.MemberWithInfoDto;
import com.back.domain.member.member.dto.MemberWithRankDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MemberController", description = "회원 관련 컨트롤러 엔드 포인트")
public class MemberController {
    private final MemberService memberService;
    private final Rq rq;


    record JoinReqBody(
            @NotBlank
            @Size(min = 2, max = 30, message = "이름은 최소 2자 이상이어야 합니다.")
            String name,

            @NotBlank
            @Size(min = 10, max = 50)
            String password,

            @NotBlank
            @Email(message = "유효한 이메일 형식이어야 합니다.")
            String email
    ) {
    }

    // 회원가입
    @PostMapping(value = "/join", produces = "application/json;charset=UTF-8")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원 가입")
    public RsData<MemberDto> join(@RequestBody @Valid JoinReqBody reqBody) {

        memberService.findByEmail(reqBody.email())
                .ifPresent(member -> {
                    throw new ServiceException(409, "이미 존재하는 이메일입니다.");
                });

        // 회원 가입 진행
        Member member = memberService.join(reqBody.name(), reqBody.password(), reqBody.email());

        return new RsData<>(
                201,
                "%s님 환영합니다. 회원 가입이 완료되었습니다.".formatted(member.getName()),
                new MemberDto(member)
        );
    }


    // 로그인 요청 시 (이메일, 비밀번호)
    record LoginReqBody(
            @NotBlank
            @Email(message = "유효한 이메일 형식이어야 합니다.")
            String email,

            @NotBlank
            String password
    ) {
    }

    // 로그인 응답 시 (MemberWithAuthDto, apiKey, accessToken)
    record LoginResBody(
            MemberWithAuthDto member,
            String apiKey,
            String accessToken
    ) {
    }

    // 로그인
    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "회원 로그인")
    public RsData<LoginResBody> login(@RequestBody @Valid LoginReqBody reqBody) {

        // 이메일로 회원 조회
        Member member = memberService.findByEmail(reqBody.email()).orElseThrow(
                () -> new ServiceException(401, "존재하지 않는 이메일입니다.")
        );

        // 비밀번호 검증
        if (!memberService.checkPassword(reqBody.password(), member.getPassword())) {
            throw new ServiceException(401, "비밀번호가 일치하지 않습니다.");
        }

        // JWT 토큰 생성
        String accessToken = memberService.genAccessToken(member);

        // 쿠키 설정
        rq.setCrossDomainCookie("accessToken", accessToken, 60 * 20);
        rq.setCrossDomainCookie("apiKey", member.getApiKey(), 60 * 60 * 24 * 7);

        return new RsData<>(
                200,
                "%s님 환영합니다.".formatted(member.getName()),
                new LoginResBody(
                        new MemberWithAuthDto(member),
                        member.getApiKey(),
                        accessToken
                )
        );
    }

    @Operation(summary = "회원 로그아웃")
    @DeleteMapping("/logout")
    public RsData<Void> logout() {

        rq.deleteCrossDomainCookie("accessToken");
        rq.deleteCrossDomainCookie("apiKey");

        return new RsData<>(
                200,
                "로그아웃 성공",
                null
        );
    }

    @Operation(summary = "회원 정보 조회 = 마이페이지")
    @GetMapping("/info")
    @Transactional(readOnly = true)
    public RsData<MemberWithInfoDto> myInfo() {

        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        Member member = memberService.findById(actor.getId())
                .orElseThrow(() -> new ServiceException(404, "존재하지 않는 회원입니다."));

        return new RsData<>(
                200,
                "내 정보 조회 완료",
                new MemberWithInfoDto(member)
        );
    }

    record ModifyReqBody(@NotBlank
                         @Size(min = 2, max = 30, message = "이름은 최소 2자 이상이어야 합니다.")
                         String name,
                         @NotBlank
                         @Size(min = 10, max = 50)
                         String password,
                         @NotBlank
                         @Email(message = "유효한 이메일 형식이어야 합니다.")
                         String email) {
    }

    @Operation(summary = "회원 정보 수정 (이름,비밀번호,메일)")
    @PutMapping("/info")
    @Transactional
    public RsData<MemberWithAuthDto> modifyInfo(@RequestBody @Valid ModifyReqBody reqBody) {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        Member member = memberService.findById(actor.getId())
                .orElseThrow(() -> new ServiceException(404, "존재하지 않는 회원입니다."));

        // 이메일 중복 체크
        if(!member.getEmail().equals(reqBody.email())) {
            memberService.findByEmail(reqBody.email())
                    .ifPresent(_member -> {
                        throw new ServiceException(409, "이미 존재하는 이메일입니다.");
                    });
        }

        memberService.modify(member, reqBody.name(), reqBody.password(), reqBody.email());

        return new RsData<>(
                200,
                "회원 정보 수정 완료",
                new MemberWithAuthDto(member)
        );

    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/withdraw")
    @Transactional
    public RsData<Void> withdraw() {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }
        Member member = memberService.findById(actor.getId())
                .orElseThrow(() -> new ServiceException(404, "존재하지 않는 회원입니다."));

        memberService.withdraw(member);

        rq.deleteCrossDomainCookie("apiKey");
        rq.deleteCrossDomainCookie("accessToken");

        return new RsData<>(
                200,
                "회원 탈퇴가 완료되었습니다.",
                null
        );
    }

    @Operation(summary = "회원 경험치순으로 5명까지 조회")
    @GetMapping("/rank")
    @Transactional(readOnly = true)
    public RsData<List<MemberWithRankDto>> rank() {

        List<MemberWithRankDto> members = memberService.getTop5MembersByExp();

        return new RsData<>(
                200,
                "경험치 순위 조회 완료",
                members
        );
    }
}