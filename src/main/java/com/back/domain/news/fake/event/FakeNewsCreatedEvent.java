package com.back.domain.news.fake.event;

import java.util.List;

public class FakeNewsCreatedEvent {
    private final List<Long> realNewsIds;

    public FakeNewsCreatedEvent(List<Long> realNewsIds) {
        this.realNewsIds = realNewsIds;
    }

    public List<Long> getRealNewsIds() {
        return realNewsIds;
    }
}
