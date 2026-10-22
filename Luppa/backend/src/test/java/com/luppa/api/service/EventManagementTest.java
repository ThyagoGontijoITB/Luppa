package com.luppa.api.service;

import com.luppa.api.dto.*;
import com.luppa.api.entity.*;
import com.luppa.api.exception.*;
import com.luppa.api.repository.*;
import com.luppa.api.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EventManagementTest {
    final EventRepository events = mock(EventRepository.class);
    final CategoryRepository categories = mock(CategoryRepository.class);
    final EventDeletionGuard guard = mock(EventDeletionGuard.class);
    final EventService service = new EventService(events, categories, mock(AccountEmailService.class), mock(EventResponseFactory.class), guard);

    AuthUser auth(int id, UserRole role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        Role type = new Role(); type.setName(role); user.setRole(type);
        return new AuthUser(user);
    }
    Event event() {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "id", 10);
        event.setCreator(auth(1, UserRole.CREATOR).user());
        event.setStartDateTime(LocalDateTime.now().plusDays(10));
        event.setEndDateTime(event.getStartDateTime().plusHours(2));
        event.setStatus(EventStatus.PUBLISHED);
        when(events.findById(10)).thenReturn(Optional.of(event));
        when(events.findByIdForUpdate(10)).thenReturn(Optional.of(event));
        return event;
    }
    EventUpdateRequest update(Event event) {
        when(categories.findBySlug("musica")).thenReturn(Optional.of(new Category()));
        return new EventUpdateRequest("Novo título", "Descrição", "musica", "Local",
            new EventAddressRequest("06400000", "Rua", 1, "Barueri", "SP", "Brasil"), null, null,
            event.getStartDateTime(), event.getEndDateTime(), EventStatus.PUBLISHED);
    }
    @Test void anotherCreatorCannotEditOrDelete() {
        Event event = event();
        assertThatThrownBy(() -> service.update(10, update(event), auth(2, UserRole.CREATOR))).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.delete(10, auth(2, UserRole.CREATOR))).isInstanceOf(ForbiddenOperationException.class);
        verify(events, never()).delete(any());
        verifyNoInteractions(guard);
    }
    @Test void ownerEditsAreSubmittedForModerationWithoutChangingOwnership() {
        Event event = event();
        service.update(10, update(event), auth(1, UserRole.CREATOR));
        assertThat(event.getTitle()).isEqualTo("Novo título");
        assertThat(event.getStatus()).isEqualTo(EventStatus.REVIEW);
        assertThat(event.getCreator().getId()).isEqualTo(1);
    }
    @Test void adminCanEditWithoutLosingPublication() {
        Event event = event();
        service.update(10, update(event), auth(9, UserRole.ADMIN));
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }
    @Test void ownerDeletesOnlyAfterDependencyCheckAndFlushesForConstraintErrors() {
        Event event = event();
        service.delete(10, auth(1, UserRole.CREATOR));
        var order = inOrder(guard, events);
        order.verify(events).findByIdForUpdate(10);
        order.verify(guard).check(10);
        order.verify(events).delete(event);
        order.verify(events).flush();
    }
    @Test void dependentHistoryPreventsDeletion() {
        event();
        doThrow(new ConflictException("linked history")).when(guard).check(10);
        assertThatThrownBy(() -> service.delete(10, auth(1, UserRole.CREATOR))).isInstanceOf(ConflictException.class);
        verify(events, never()).delete(any());
    }
}
