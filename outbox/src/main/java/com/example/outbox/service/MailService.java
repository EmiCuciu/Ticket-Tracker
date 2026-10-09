package com.example.outbox.service;

import com.example.outbox.domain.Email;

public interface MailService {
    void send(Email email);
}