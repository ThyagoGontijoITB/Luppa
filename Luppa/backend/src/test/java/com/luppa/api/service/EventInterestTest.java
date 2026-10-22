package com.luppa.api.service;

import com.luppa.api.entity.*;
import com.luppa.api.repository.*;
import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EventInterestTest {
    final SavedEventRepository saves = mock(SavedEventRepository.class);
    final EventRepository events = mock(EventRepository.class);
    final EventResponseFactory responses = new EventResponseFactory(saves);

    Event event(int id) {
        Event event = new Event(); ReflectionTestUtils.setField(event, "id", id);
        Category category = new Category(); ReflectionTestUtils.setField(category, "id", 3); category.setName("Música");
        event.setCategory(category);
        User user = new User(); ReflectionTestUtils.setField(user, "id", 1);
        Role role = new Role(); role.setName(UserRole.CREATOR); user.setRole(role);
        event.setCreator(user);
        return event;
    }
    @Test void listsUseOneGroupedDatabaseQueryAndPreserveZeroCounts() {
        Event first = event(10), second = event(20);
        SavedEventRepository.InterestCount count = mock(SavedEventRepository.InterestCount.class);
        when(count.getEventId()).thenReturn(10); when(count.getTotal()).thenReturn(4L);
        when(saves.countInterests(List.of(10,20))).thenReturn(List.of(count));
        var page = responses.page(new PageImpl<>(List.of(first, second)));
        assertThat(page.getContent()).extracting(item -> item.interestCount()).containsExactly(4L, 0L);
        verify(saves).countInterests(List.of(10,20)); verify(saves, never()).countInterest(anyInt());
    }
    @Test void repeatedSaveIsIdempotentAndLockedBeforeCheckingExistingSave() {
        Event event = event(10);
        SavedEvent saved = new SavedEvent(); saved.setEvent(event); saved.setUser(event.getCreator());
        ReflectionTestUtils.setField(saved, "id", 5);
        when(events.findByIdForUpdate(10)).thenReturn(Optional.of(event));
        when(saves.findByEventIdAndUserId(10, 1)).thenReturn(Optional.of(saved));
        when(saves.countInterest(10)).thenReturn(1L);
        var service = new SavedEventService(saves, events, responses);
        assertThat(service.save(10, new AuthUser(event.getCreator())).event().interestCount()).isEqualTo(1);
        service.save(10, new AuthUser(event.getCreator()));
        verify(saves, never()).saveAndFlush(any());
        var order = inOrder(events, saves);
        order.verify(events).findByIdForUpdate(10);
        order.verify(saves).findByEventIdAndUserId(10,1);
    }
    @Test void unsaveIsScopedToAuthenticatedUserAndSerializedWithSave() {
        Event event = event(10); when(events.findByIdForUpdate(10)).thenReturn(Optional.of(event));
        new SavedEventService(saves, events, responses).unsave(10, new AuthUser(event.getCreator()));
        var order = inOrder(events, saves);
        order.verify(events).findByIdForUpdate(10);
        order.verify(saves).deleteByEventIdAndUserId(10,1);
    }
    @Test void deletionGuardProtectsEachKnownDependency() {
        var ratings = mock(RatingRepository.class);
        var reports = mock(ReportRepository.class);
        var links = mock(EventSocialLinkRepository.class);
        var guard = new EventDeletionGuard(saves,ratings,reports,links);
        for (int i=0;i<4;i++) {
            reset(saves,ratings,reports,links);
            if(i==0) when(saves.existsByEventId(10)).thenReturn(true);
            if(i==1) when(ratings.existsByEventId(10)).thenReturn(true);
            if(i==2) when(reports.existsByEventId(10)).thenReturn(true);
            if(i==3) when(links.existsByIdEventId(10)).thenReturn(true);
            assertThatThrownBy(() -> guard.check(10)).isInstanceOf(com.luppa.api.exception.ConflictException.class);
        }
        reset(saves,ratings,reports,links);
        assertThatCode(() -> guard.check(10)).doesNotThrowAnyException();
    }
}
