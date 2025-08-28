package com.back.global.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class SecurityUser extends User implements OAuth2User {

    private long id;

    private String name;

    private String email;

    public SecurityUser(
            long id,
            String email,
            String name,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(email, password , authorities);
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }
}
