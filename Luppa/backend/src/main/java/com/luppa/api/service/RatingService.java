package com.luppa.api.service;

import com.luppa.api.dto.RatingRequest;
import com.luppa.api.dto.RatingResponse;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.Rating;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.RatingMapper;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.repository.RatingRepository;
import com.luppa.api.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RatingService {
    private final RatingRepository ratingRepository;
    private final EventRepository eventRepository;

    public RatingService(RatingRepository ratingRepository, EventRepository eventRepository) {
        this.ratingRepository = ratingRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> findByEvent(Integer eventId) {
        return ratingRepository.findPublicByEventIdOrderByRatedAtDesc(eventId).stream()
                .map(RatingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> findMine(AuthUser authUser) {
        return ratingRepository.findByUserIdOrderByRatedAtDesc(authUser.idUsuario()).stream()
                .map(RatingMapper::toResponse)
                .toList();
    }

    @Transactional
    public RatingResponse upsert(Integer eventId, RatingRequest request, AuthUser authUser) {
        validateScore(request.score());
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        validateEventEnded(event);
        Rating rating = ratingRepository.findByEventIdAndUserId(eventId, authUser.idUsuario())
                .orElseGet(() -> {
                    Rating created = new Rating();
                    created.setEvent(event);
                    created.setUser(authUser.user());
                    return created;
                });
        rating.setScore(request.score().byteValue());
        rating.setDescription(request.description() == null ? "" : request.description().trim());
        return RatingMapper.toResponse(ratingRepository.save(rating));
    }

    @Transactional
    public void delete(Integer eventId, AuthUser authUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        validateEventEnded(event);
        ratingRepository.deleteByEventIdAndUserId(eventId, authUser.idUsuario());
    }

    private void validateScore(Integer score) {
        if (score == null || score < 1 || score > 5) {
            throw new InvalidRequestException("Rating score must be between 1 and 5");
        }
    }

    private void validateEventEnded(Event event) {
        if (event.getEndDateTime() == null || event.getEndDateTime().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Event ratings are available only after the event has ended");
        }
    }
}
