package com.demo.upimesh.model;

import java.time.Instant;

public record MeshPacket(String packetId, int ttl, Instant createdAt, String ciphertext) {}
