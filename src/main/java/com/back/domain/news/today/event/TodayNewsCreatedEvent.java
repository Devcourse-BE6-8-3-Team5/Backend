package com.back.domain.news.today.event;

public class TodayNewsCreatedEvent {
    private final Long todayNewsId;

    public TodayNewsCreatedEvent(Long todayNewsId) {
        this.todayNewsId = todayNewsId;
    }

    public Long getTodayNewsId() {
        return todayNewsId;
    }
}
