package com.luppa.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EventAddressRequest(
        @NotBlank @Pattern(regexp = "\\d{8}", message = "CEP must contain exactly 8 digits") String cep,
        @Size(max = 100) @NotBlank String rua,
        @PositiveOrZero @NotNull Integer numero,
        @Size(max = 40) @NotBlank String cidade,
        @Size(max = 30) @NotBlank String estado,
        @Size(max = 40) @NotBlank String pais
) {
}
