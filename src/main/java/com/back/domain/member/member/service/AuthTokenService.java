package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.standard.util.Ut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthTokenService {
    @Value("${custom.jwt.secretKey}")
    private String jwtSecretKey;

    @Value("${custom.accessToken.expirationSeconds}")
    private int accessTokenExpirationSeconds;

    String genAccessToken(Member member) {
        long id = member.getId();
        String email = member.getEmail();
        String name = member.getName();
        String role = member.getRole();

        return Ut.jwt.toString(
                jwtSecretKey,
                accessTokenExpirationSeconds,
                Map.of("id", id, "email", email, "name", name, "role", role)
        );
    }

    Map<String, Object> payload(String accessToken) {
        Map<String, Object> parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken);

        if (parsedPayload == null) return null;

        long id = ((Number) parsedPayload.get("id")).longValue();
        String email = (String) parsedPayload.get("email");
        String name = (String) parsedPayload.get("name");
        String role = (String) parsedPayload.get("role");

        return Map.of("id", (long)id, "email", email, "name", name, "role", role);
    }
}

