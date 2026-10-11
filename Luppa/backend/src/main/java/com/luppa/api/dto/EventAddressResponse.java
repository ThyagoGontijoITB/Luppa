package com.luppa.api.dto;

public record EventAddressResponse(
        String cep,
        String rua,
        Integer numero,
        String cidade,
        String estado,
        String pais
) {
}
