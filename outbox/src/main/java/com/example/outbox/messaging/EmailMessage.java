package com.example.outbox.messaging;

import java.util.UUID;

public record EmailMessage(UUID emailId) {
}