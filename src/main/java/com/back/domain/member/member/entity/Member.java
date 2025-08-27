package com.back.domain.member.member.entity;

import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.PRIVATE)
    @EqualsAndHashCode.Include
    private long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Min(0)
    private int exp; //경험치

    @Min(1)
    private int level; //레벨

    @Column(nullable = false)
    private String role; // "USER" 또는 "ADMIN"

    @Column(nullable = false, unique = true)
    private String apiKey; // 리프레시 토큰

    

    @Column(unique = true, nullable = true)
    private String oauthId; // 소셜 로그인용 고유 oauthId

    // 유저가 푼 퀴즈 기록을 저장하는 리스트 일단 엔티티 없어서 주석
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JsonIgnore
    private List<QuizHistory> quizHistories = new ArrayList<>(); //유저가 퀴즈를 푼 기록


    public Member(long id, String email, String name) {
        setId(id);
        this.email = email;
        setName(name);
    }

    public Member(long id, String email, String name, String role) {
        setId(id);
        this.email = email;
        setName(name);
        this.role = role;
    }

    //role과 "ADMIN"을 대소문자 구분 없이 비교하여 ADMIN이면 true 반환
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    private List<String> getAuthoritiesAsStringList() {
        List<String> authorities = new ArrayList<>();
        if (isAdmin()) {
            authorities.add("ADMIN");
        } else {
            authorities.add("USER");
        }
        return authorities;
    }

    // Member의 role을 Security가 사용하는 ROLE_ADMIN, ROLE_USER 형태로 변환
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthoritiesAsStringList()
                .stream()
                .map(auth -> new SimpleGrantedAuthority("ROLE_" + auth))
                .toList();
    }

    
}
