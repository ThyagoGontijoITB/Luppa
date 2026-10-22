package com.luppa.api.service;

import com.luppa.api.dto.SavedEventResponse;
import com.luppa.api.dto.SavedStatusResponse;
import com.luppa.api.mapper.SavedEventMapper;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.SavedEvent;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.repository.SavedEventRepository;
import com.luppa.api.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SavedEventService {
    private final SavedEventRepository savedEventRepository;
    private final EventRepository eventRepository;
    private final EventResponseFactory responses;

    public SavedEventService(SavedEventRepository savedEventRepository, EventRepository eventRepository, EventResponseFactory responses) {
        this.savedEventRepository = savedEventRepository;
        this.eventRepository = eventRepository;
        this.responses = responses;
    }

    @Transactional
    public SavedEventResponse save(Integer eventId, AuthUser authUser) {
        // Serialize save/unsave for this event. The existing unique constraint remains the final guard.
        Event event = lockEvent(eventId);
        return savedEventRepository.findByEventIdAndUserId(eventId, authUser.idUsuario())
                .map(this::toResponse)
                .orElseGet(() -> {
                    SavedEvent savedEvent = new SavedEvent();
                    savedEvent.setEvent(event);
                    savedEvent.setUser(authUser.user());
                    return toResponse(savedEventRepository.saveAndFlush(savedEvent));
                });
    }

    @Transactional
    public void unsave(Integer eventId, AuthUser authUser) {
        lockEvent(eventId);
        savedEventRepository.deleteByEventIdAndUserId(eventId, authUser.idUsuario());
    }

    private Event lockEvent(Integer id) {
        return eventRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private SavedEventResponse toResponse(SavedEvent saved) {
        return SavedEventMapper.toResponse(saved, responses.one(saved.getEvent()));
    }

    @Transactional(readOnly = true)
    public List<SavedEventResponse> findMine(AuthUser authUser) {
        var saved = savedEventRepository.findByUserIdOrderBySavedAtDesc(authUser.idUsuario());
        var mapper = responses.mapper(saved.stream().map(SavedEvent::getEvent).toList(), false);
        return saved.stream().map(item -> SavedEventMapper.toResponse(item, mapper.apply(item.getEvent()))).toList();
    }

    @Transactional(readOnly = true)
    public SavedStatusResponse status(Integer eventId, AuthUser authUser) {
        boolean saved = savedEventRepository.existsByEventIdAndUserId(eventId, authUser.idUsuario());
        return new SavedStatusResponse(eventId.longValue(), saved);
    }
}
