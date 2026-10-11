package com.luppa.api.mapper;

import com.luppa.api.dto.SavedEventResponse;
import com.luppa.api.entity.SavedEvent;

public final class SavedEventMapper {
    private SavedEventMapper() {
    }

    public static SavedEventResponse toResponse(SavedEvent savedEvent) {
        return new SavedEventResponse(
                savedEvent.getId().longValue(),
                EventMapper.toResponse(savedEvent.getEvent()),
                savedEvent.getSavedAt()
        );
    }
}
