package com.back.domain.member.quizhistory.entity

import com.back.domain.member.member.entity.Member
import com.back.domain.quiz.QuizType
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "quiz_history",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "quiz_id", "quiz_type"])]
)
@EntityListeners(AuditingEntityListener::class)
class QuizHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var member: Member? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var quizType: QuizType,

    @Column(nullable = false)
    var quizId: Long? =null,

    @CreatedDate
    var createdDate: LocalDateTime? = null,

    @Column(nullable = false)
    var answer: String,

    @Column(nullable = false)
    var isCorrect: Boolean,

    @Column(nullable = false)
    var gainExp: Int

) {

    constructor(member: Member,id: Long?, quizType: QuizType, answer: String, isCorrect: Boolean, gainExp: Int) : this(
        null,
        member,
        quizType,
        id,
        null,
        answer,
        isCorrect,
        gainExp
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuizHistory) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

}
