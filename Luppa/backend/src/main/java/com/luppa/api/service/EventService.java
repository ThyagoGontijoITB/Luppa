package com.luppa.api.service;

import com.luppa.api.dto.EventCreateRequest;
import com.luppa.api.dto.EventAddressRequest;
import com.luppa.api.dto.EventResponse;
import com.luppa.api.dto.EventStatusUpdateRequest;
import com.luppa.api.dto.EventUpdateRequest;
import com.luppa.api.entity.Address;
import com.luppa.api.entity.Category;
import com.luppa.api.entity.Event;
import com.luppa.api.entity.EventContact;
import com.luppa.api.entity.EventStatus;
import com.luppa.api.entity.StatusEvent;
import com.luppa.api.entity.User;
import com.luppa.api.entity.UserRole;
import com.luppa.api.exception.ForbiddenOperationException;
import com.luppa.api.exception.InvalidRequestException;
import com.luppa.api.exception.ResourceNotFoundException;
import com.luppa.api.mapper.EventMapper;
import com.luppa.api.repository.CategoryRepository;
import com.luppa.api.repository.EventRepository;
import com.luppa.api.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final AccountEmailService emailService;

    public EventService(EventRepository eventRepository, CategoryRepository categoryRepository, AccountEmailService emailService) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> search(String search, String category, EventStatus status, Pageable pageable) {
        Integer categoryId = blankToNull(category) == null ? null : categoryRepository.findBySlug(category)
                .map(Category::getId)
                .orElse(null);
        Integer statusId = status == null ? null : StatusEvent.idFor(status);
        return eventRepository.search(blankToNull(search), categoryId, statusId, pageable)
                .map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findFeatured() {
        Integer publishedStatusId = StatusEvent.idFor(EventStatus.PUBLISHED);
        return eventRepository.findFirstPublishedManualFeatured(publishedStatusId)
                .or(() -> eventRepository.findNearestPublishedUpcoming(
                        publishedStatusId,
                        LocalDateTime.now(),
                        PageRequest.of(0, 1)
                ).stream().findFirst())
                .map(EventMapper::toFeaturedResponse)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findMine(AuthUser authUser, Pageable pageable) {
        return eventRepository.findByCreatorId(authUser.idUsuario(), pageable).map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse get(Integer id) {
        return EventMapper.toResponse(find(id));
    }

    @Transactional
    public EventResponse create(EventCreateRequest request, AuthUser authUser) {
        requireCreatorOrAdmin(authUser);
        validateDates(request.startDateTime(), request.endDateTime());

        Event event = new Event();
        event.setCreator(authUser.user());
        event.setContact(EventContact.fromUser(authUser.user()));
        apply(event, request.title(), request.description(), request.categorySlug(), request.address(),
                request.latitude(), request.longitude(), request.startDateTime(), request.endDateTime(),
                request.status() == null ? EventStatus.DRAFT : request.status());

        return EventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Integer id, EventUpdateRequest request, AuthUser authUser) {
        Event event = find(id);
        requireOwnerOrAdmin(event, authUser);
        validateDatesForUpdate(event, request.startDateTime(), request.endDateTime());
        apply(event, request.title(), request.description(), request.categorySlug(), request.address(),
                request.latitude(), request.longitude(), request.startDateTime(), request.endDateTime(), request.status());
        return EventMapper.toResponse(event);
    }

    @Transactional
    public EventResponse updateStatus(Integer id, EventStatusUpdateRequest request, AuthUser authUser) {
        Event event = find(id);
        requireAdmin(authUser);
        EventStatus previousStatus = event.getStatus();
        event.setStatus(request.status());
        if (previousStatus != request.status()) {
            if (request.status() == EventStatus.PUBLISHED) {
                emailService.eventPublished(event.getCreator(), event.getId(), event.getTitle());
            } else if (request.status() == EventStatus.CANCELLED) {
                emailService.eventRejected(event.getCreator(), event.getTitle());
            }
        }
        return EventMapper.toResponse(event);
    }

    @Transactional
    public EventResponse updateFeatured(Integer id, AuthUser authUser) {
        requireAdmin(authUser);
        Event event = find(id);
        requireFeaturedEligible(event);
        eventRepository.clearManualFeatured();
        Event target = find(id);
        target.setDestaquePrincipal(true);
        return EventMapper.toResponse(findForResponse(target.getId()));
    }

    @Transactional
    public void clearFeatured(AuthUser authUser) {
        requireAdmin(authUser);
        eventRepository.clearManualFeatured();
    }

    @Transactional
    public void delete(Integer id, AuthUser authUser) {
        Event event = find(id);
        requireOwnerOrAdmin(event, authUser);
        eventRepository.delete(event);
    }

    private Event find(Integer id) {
        return eventRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private Event findForResponse(Integer id) {
        return eventRepository.findByIdForResponse(id).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private void apply(Event event, String title, String description, String categorySlug, EventAddressRequest addressRequest,
                       java.math.BigDecimal latitude, java.math.BigDecimal longitude,
                       java.time.LocalDateTime start, java.time.LocalDateTime end, EventStatus status) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        event.setTitle(title);
        event.setDescription(description);
        event.setCategory(category);
        applyAddress(event, addressRequest);
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setStatus(status);
    }

    private void validateDates(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (start == null || end == null) {
            throw new InvalidRequestException("Event start and end date/time are required");
        }
        if (start.isBefore(now)) {
            throw new InvalidRequestException("Event start date cannot be in the past");
        }
        if (start.toLocalDate().isAfter(now.toLocalDate().plusYears(1))) {
            throw new InvalidRequestException("Event start date cannot be more than one year in the future");
        }
        if (end.isBefore(start)) {
            throw new InvalidRequestException("Event end date cannot be before start date");
        }
    }

    private void validateDatesForUpdate(Event event, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            throw new InvalidRequestException("Event start and end date/time are required");
        }
        if (end.isBefore(start)) {
            throw new InvalidRequestException("Event end date cannot be before start date");
        }
        boolean datesChanged = !start.equals(event.getStartDateTime()) || !end.equals(event.getEndDateTime());
        if (datesChanged) {
            validateDates(start, end);
        }
    }

    private void applyAddress(Event event, EventAddressRequest request) {
        Address address = event.getAddress() == null ? new Address() : event.getAddress();
        address.setZipCode(request.cep());
        address.setStreet(request.rua());
        address.setNumber(request.numero());
        address.setCity(request.cidade());
        address.setState(request.estado());
        address.setCountry(request.pais());
        event.setAddress(address);
    }

    private void requireCreatorOrAdmin(AuthUser authUser) {
        User user = authUser.user();
        UserRole role = user.getRole().getName();
        if (role != UserRole.CREATOR && !role.isAdminLike()) {
            throw new ForbiddenOperationException("Only creators or admins can create events");
        }
    }

    private void requireAdmin(AuthUser authUser) {
        User user = authUser.user();
        if (!user.getRole().getName().isAdminLike()) {
            throw new ForbiddenOperationException("Only admins can update event status");
        }
    }

    private void requireFeaturedEligible(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidRequestException("Only published events can be featured");
        }
    }

    private void requireOwnerOrAdmin(Event event, AuthUser authUser) {
        User user = authUser.user();
        if (user.getRole().getName().isAdminLike()) {
            return;
        }
        if (!event.getCreator().getId().equals(authUser.idUsuario())) {
            throw new ForbiddenOperationException("Creators can only manage their own events");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
