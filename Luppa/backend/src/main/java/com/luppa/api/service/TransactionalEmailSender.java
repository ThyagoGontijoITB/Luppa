package com.luppa.api.service;

public interface TransactionalEmailSender {
    void send(TransactionalEmailMessage message);
}
