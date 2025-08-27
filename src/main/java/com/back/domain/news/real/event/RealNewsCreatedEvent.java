package com.back.domain.news.real.event;

import java.util.List;

public class RealNewsCreatedEvent {
    private final List<Long> realNewsIds;

    public RealNewsCreatedEvent(List<Long> realNewsIds) {
        this.realNewsIds = realNewsIds;
    }

    public List<Long> getRealNewsIds() {
        return realNewsIds;
    }
}
