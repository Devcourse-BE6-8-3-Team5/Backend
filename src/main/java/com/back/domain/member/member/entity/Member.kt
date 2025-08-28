package com.back.domain.member.member.entity

import com.back.domain.member.quizhistory.entity.QuizHistory
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.Min
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

@Entity
class Member() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var _id: Long? = null

    var id: Long
        get() = _id ?: 0
        set(value) {
            _id = value
        }

    @Column(nullable = false, length = 50)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var password: String

    @Column(nullable = false, unique = true)
    lateinit var email: String

    var exp: @Min(0) Int = 0 //경험치

    var level: @Min(1) Int = 1 //레벨

    @Column(nullable = false)
    lateinit var role: String // "USER" 또는 "ADMIN"

    @Column(nullable = false, unique = true)
    lateinit var apiKey: String // 리프레시 토큰

    @Column(unique = true, nullable = true)
    var oauthId: String? = null // 소셜 로그인용 고유 oauthId

    // 유저가 푼 퀴즈 기록을 저장하는 리스트 일단 엔티티 없어서 주석
    @OneToMany(
        mappedBy = "member",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.REMOVE],
        orphanRemoval = true
    )
    @JsonIgnore
    private val quizHistories = mutableListOf<QuizHistory>()


    constructor(id: Long, email: String, name: String, role: String) : this() {
        this.id = id
        this.email = email
        this.name = name
        this.role = role
    }

    constructor(
        name: String, email: String, password: String, exp: Int, level: Int,
        role: String, apikey: String, oauthId: String?
    ) : this() {
        this.name = name
        this.password = password
        this.email = email
        this.exp = exp
        this.level = level
        this.role = role
        this.apiKey = apikey
        this.oauthId = oauthId
    }

    val isAdmin: Boolean
        //role과 "ADMIN"을 대소문자 구분 없이 비교하여 ADMIN이면 true 반환
        get() = "ADMIN".equals(role, ignoreCase = true)

    private val authoritiesAsStringList: MutableList<String>
        get() {
            val authorities = mutableListOf<String>()
            if (this.isAdmin) {
                authorities.add("ADMIN")
            } else {
                authorities.add("USER")
            }
            return authorities
        }

    val authorities: MutableCollection<out GrantedAuthority>
        get() = authoritiesAsStringList
            .map { auth -> SimpleGrantedAuthority("ROLE_$auth") }
            .toMutableList()


}
