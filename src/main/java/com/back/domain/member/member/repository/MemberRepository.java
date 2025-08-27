package com.back.domain.member.member.repository;

import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByApiKey(String apiKey);

    Optional<Member> findByName(String name);

    Optional<Member> findByOauthId(String oauthId);

    List<Member> findTop5ByOrderByExpDesc();
}