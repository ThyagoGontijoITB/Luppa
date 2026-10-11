package com.luppa.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserMessageRequest(
        @NotBlank(message = "Informe o assunto.")
        @Size(min = 5, max = 150, message = "O assunto deve ter entre 5 e 150 caracteres.")
        String subject,

        @NotBlank(message = "Informe a mensagem.")
        @Size(min = 10, max = 5000, message = "A mensagem deve ter entre 10 e 5000 caracteres.")
        String message
) {
}
