package com.luppa.api.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.");
    }
}
