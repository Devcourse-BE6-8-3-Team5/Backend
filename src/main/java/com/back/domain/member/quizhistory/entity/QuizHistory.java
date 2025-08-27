package com.back.domain.member.quizhistory.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.quiz.QuizType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
        name = "quiz_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "quiz_id", "quizType"})
)
public class QuizHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.PRIVATE)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @Setter
    Member member; //member_id

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    QuizType quizType; // 퀴즈 타입(3가지)

    @Column(nullable = false)
    private Long quizId; // 퀴즈 ID

    @CreatedDate
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime createdDate; // 퀴즈 풀이 시간

    @Column(nullable = false)
    String answer; // 유저 정답

    @Column(nullable = false)
    boolean isCorrect; // 퀴즈 정답 여부

    @Column(nullable = false)
    int gainExp; // 퀴즈 풀이로 얻은 경험치

}
