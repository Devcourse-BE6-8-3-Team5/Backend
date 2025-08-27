package com.back.domain.news.fake.entity;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FakeNews {

    /**
     @Id
     @OneToOne
     @MapsId
     @JoinColumn(name = "real_news_id")
     private RealNews realNews;

     이런 식으로 처리를 해버리면 @Id가 realNews 타입 필드에 붙기 떄문에
     기본키가 RealNews 타입이라고 jpa가 생각해서 => RealNews는 엔티티이므로 복합키일 수 있다 라고 해석한다
     따라서 명시적 ID 필드를 만들어서 FakeNews의 PK로 사용하고
     RealNews의 ID를 FakeNews의 PK로 사용하도록 한다.
     */
    @Id
    @Column(name = "real_news_id")
    private Long id; // RealNews의 ID를 FakeNews의 PK로 사용

    @OneToOne
    @MapsId // 진짜뉴스의 ID를 이 엔티티의 PK로 사용
    @JoinColumn(name = "real_news_id")
    @JsonIgnore
    private RealNews realNews;

    @Lob
    private String content;

    @OneToMany(mappedBy = "fakeNews", cascade = ALL, orphanRemoval = true)
    @JsonIgnore
    private List<FactQuiz> factQuizzes = new ArrayList<>();

}
