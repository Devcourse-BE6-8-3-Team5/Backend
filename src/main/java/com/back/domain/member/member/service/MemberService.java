package com.back.domain.member.member.service;

import com.back.domain.member.member.dto.MemberWithRankDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    // 기본 회원가입
    public Member join(String name, String password, String email) {
        String encodedPassword = passwordEncoder.encode(password);

        String role = "USER";
        if("admin".equalsIgnoreCase(name) || "system".equalsIgnoreCase(name)) {
            role = "ADMIN"; // 관리자 권한 설정
        }

        Member member = Member.builder()
                .name(name)
                .password(encodedPassword)
                .email(email)
                .exp(0)
                .level(1)
                .role(role) //기본 권한 USER. 관리자면 "ADMIN"으로 설정하시면 됩니다
                .apiKey(UUID.randomUUID().toString())
                .oauthId(null)
                .build();

        return memberRepository.save(member);
    }

    // 소셜로그인으로 회원가입 & 회원 정보 수정
    public RsData<Member> modifyOrJoin(String oauthId, String email, String nickname) {

        //oauthId로 기존 회원인지 확인
        Member member = memberRepository.findByOauthId(oauthId).orElse(null);

        // 기존 회원이 아니면 소셜로그인으로 회원가입 진행
        if(member == null) {
            member = joinSocial(oauthId, email, nickname);
            return new RsData<>(201, "회원가입이 완료되었습니다.", member);
        }

        // 기존 회원이면 회원 정보 수정
        modifySocial(member, nickname);
        return new RsData<>(200, "회원 정보가 수정되었습니다.", member);
    }

    public Member joinSocial(String oauthId, String email, String nickname){
        memberRepository.findByOauthId(oauthId)
                .ifPresent(_member -> {
                    throw new ServiceException(409, "이미 존재하는 계정입니다.");
                });
        String encodedPassword = passwordEncoder.encode("1234");
        Member member = Member.builder()
                .name(nickname)
                .password(encodedPassword) // null이면 안돼서 임의로 1234
                .email(email)
                .exp(0)
                .level(1)
                .role("USER") //기본 권한 USER. 관리자면 "ADMIN"으로 설정하시면 됩니다
                .apiKey(UUID.randomUUID().toString())
                .oauthId(oauthId)
                .build();

        return memberRepository.save(member);
    }

    public void modifySocial(Member member, String nickname){
        member.setName(nickname);
        memberRepository.save(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }

    public Map<String, Object> payload(String accessToken) {
        return authTokenService.payload(accessToken);
    }

    public Optional<Member> findByApiKey(String apiKey) {
        return memberRepository.findByApiKey(apiKey);
    }

    public String genAccessToken(Member member) {
        return authTokenService.genAccessToken(member);
    }

    @Transactional
    public void modify(Member member, String name,  String password, String email) {
        member.setName(name);
        member.setPassword(passwordEncoder.encode(password));
        member.setEmail(email);
        memberRepository.save(member);
    }

    public void withdraw(Member member) {
        if(member.isAdmin())
            throw new ServiceException(403,"관리자는 탈퇴할 수 없습니다.");

        memberRepository.delete(member);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public long count() {
        return memberRepository.count();
    }

    public List<MemberWithRankDto> getTop5MembersByExp() {
        List<Member> members = memberRepository.findTop5ByOrderByExpDesc();

        return members.stream()
                .map(MemberWithRankDto::new)
                .toList();
    }
}
