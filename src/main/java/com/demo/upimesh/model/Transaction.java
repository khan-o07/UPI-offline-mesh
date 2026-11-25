package com.demo.upimesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions", indexes = @Index(name = "idx_packet_hash", columnList = "packetHash", unique = true))
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String receiver;
    private BigDecimal amount;
    private String packetHash;
    private Instant settledAt;

    protected Transaction() {}

    public Transaction(String sender, String receiver, BigDecimal amount, String packetHash) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.packetHash = packetHash;
        this.settledAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public BigDecimal getAmount() { return amount; }
    public String getPacketHash() { return packetHash; }
    public Instant getSettledAt() { return settledAt; }
}
