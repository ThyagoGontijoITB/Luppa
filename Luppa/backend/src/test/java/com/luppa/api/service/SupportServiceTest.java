package com.luppa.api.service;

import com.luppa.api.dto.SupportContactRequest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SupportServiceTest {
    private final AccountEmailService emails = mock(AccountEmailService.class);
    private final SupportService service = new SupportService(emails);
    private final SupportContactRequest request = new SupportContactRequest("Ana", "ana@example.com", "Contato", "Ajuda", "Mensagem");

    @Test void sendsToSupportBeforeReportingAcceptanceWithoutSendingUnsolicitedConfirmations() {
        assertThat(service.create(request, null).message()).contains("aceita");
        verify(emails).internalSupport(eq("Ana"), eq("ana@example.com"), isNull(), eq("Contato"), eq("Ajuda"), eq("Mensagem"), anyString());
        verifyNoMoreInteractions(emails);
    }

    @Test void providerFailureNeverReportsSuccess() {
        doThrow(new IllegalStateException("provider rejected" )).when(emails).internalSupport(anyString(), anyString(), isNull(), anyString(), anyString(), anyString(), anyString());
        assertThatThrownBy(() -> service.create(request, null)).isInstanceOf(IllegalStateException.class);
    }
}
