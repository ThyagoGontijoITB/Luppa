package com.luppa.api.service;

import com.luppa.api.dto.SavedEventResponse;
import com.luppa.api.dto.SavedStatusResponse;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.SavedEvent;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.SavedEventMapper;
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

    public SavedEventService(SavedEventRepository savedEventRepository, EventRepository eventRepository) {
        this.savedEventRepository = savedEventRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public SavedEventResponse save(Integer eventId, AuthUser authUser) {
        return savedEventRepository.findByEventIdAndUserId(eventId, authUser.idUsuario())
                .map(SavedEventMapper::toResponse)
                .orElseGet(() -> {
                    Event event = eventRepository.findById(eventId)
                            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
                    SavedEvent savedEvent = new SavedEvent();
                    savedEvent.setEvent(event);
                    savedEvent.setUser(authUser.user());
                    return SavedEventMapper.toResponse(savedEventRepository.save(savedEvent));
                });
    }

    @Transactional
    public void unsave(Integer eventId, AuthUser authUser) {
        savedEventRepository.deleteByEventIdAndUserId(eventId, authUser.idUsuario());
    }

    @Transactional(readOnly = true)
    public List<SavedEventResponse> findMine(AuthUser authUser) {
        return savedEventRepository.findByUserIdOrderBySavedAtDesc(authUser.idUsuario())
                .stream()
                .map(SavedEventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavedStatusResponse status(Integer eventId, AuthUser authUser) {
        boolean saved = savedEventRepository.existsByEventIdAndUserId(eventId, authUser.idUsuario());
        return new SavedStatusResponse(eventId.longValue(), saved);
    }
}
