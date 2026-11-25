package com.demo.upimesh.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentInstruction(
        String sender,
        String receiver,
        BigDecimal amount,
        String pinHash,
        String nonce,
        Instant signedAt
) {}
