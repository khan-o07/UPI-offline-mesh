package com.demo.upimesh.service;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.PaymentInstruction;
import com.demo.upimesh.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class BridgeIngestionService {
    public enum Outcome { SETTLED, DUPLICATE_DROPPED, INVALID }

    public record IngestionResult(Outcome outcome, String packetHash, String reason, Long transactionId) {}

    private final HybridCryptoService crypto;
    private final IdempotencyService idempotency;
    private final SettlementService settlement;
    private final Duration freshness;

    public BridgeIngestionService(HybridCryptoService crypto,
                                  IdempotencyService idempotency,
                                  SettlementService settlement,
                                  @Value("${demo.packet.freshness-hours:24}") long freshnessHours) {
        this.crypto = crypto;
        this.idempotency = idempotency;
        this.settlement = settlement;
        this.freshness = Duration.ofHours(freshnessHours);
    }

    public IngestionResult ingest(MeshPacket packet) {
        try {
            String packetHash = crypto.ciphertextHash(packet.ciphertext());
            if (!idempotency.claim(packetHash)) {
                return new IngestionResult(Outcome.DUPLICATE_DROPPED, packetHash, null, null);
            }

            PaymentInstruction payment = crypto.decrypt(packet.ciphertext());
            Instant signedAt = payment.signedAt();
            Instant now = Instant.now();
            if (signedAt.isBefore(now.minus(freshness)) || signedAt.isAfter(now.plusSeconds(300))) {
                return new IngestionResult(Outcome.INVALID, packetHash,
                        "Payment is outside the freshness window", null);
            }

            Transaction transaction = settlement.settle(
                    payment.sender(), payment.receiver(), payment.amount(), packetHash);
            return new IngestionResult(Outcome.SETTLED, packetHash, null, transaction.getId());
        } catch (SettlementService.DuplicateSettlementException ex) {
            return new IngestionResult(Outcome.DUPLICATE_DROPPED, "unknown", null, null);
        } catch (javax.crypto.AEADBadTagException | IllegalArgumentException ex) {
            return new IngestionResult(Outcome.INVALID, "unknown", ex.getMessage(), null);
        } catch (Exception ex) {
            return new IngestionResult(Outcome.INVALID, "unknown", ex.getMessage(), null);
        }
    }
}
