package com.luppa.api.service;

import com.luppa.api.dto.EventResponse;
import com.luppa.api.entity.Event;
import com.luppa.api.mapper.EventMapper;
import com.luppa.api.repository.SavedEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventResponseFactory {
    private final SavedEventRepository saves;
    public EventResponseFactory(SavedEventRepository saves) { this.saves = saves; }

    public EventResponse one(Event event) {
        return EventMapper.toResponse(event, event.isDestaquePrincipal(), saves.countInterest(event.getId()));
    }

    public Function<Event, EventResponse> mapper(List<Event> events, boolean featured) {
        Map<Integer, Long> counts = events.isEmpty() ? Map.of() : saves.countInterests(events.stream().map(Event::getId).toList())
                .stream().collect(Collectors.toMap(SavedEventRepository.InterestCount::getEventId, SavedEventRepository.InterestCount::getTotal));
        return event -> EventMapper.toResponse(event, featured || event.isDestaquePrincipal(), counts.getOrDefault(event.getId(), 0L));
    }

    public Page<EventResponse> page(Page<Event> events) { return events.map(mapper(events.getContent(), false)); }
    public List<EventResponse> list(List<Event> events, boolean featured) { return events.stream().map(mapper(events, featured)).toList(); }
}
