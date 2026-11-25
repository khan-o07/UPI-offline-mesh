package com.demo.upimesh;

import com.demo.upimesh.model.AccountRepository;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.PaymentInstruction;
import com.demo.upimesh.service.BridgeIngestionService;
import com.demo.upimesh.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdempotencyConcurrencyTest {
    @Autowired
    private com.demo.upimesh.crypto.HybridCryptoService crypto;
    @Autowired
    private BridgeIngestionService ingestion;
    @Autowired
    private IdempotencyService idempotency;
    @Autowired
    private AccountRepository accounts;

    @BeforeEach
    void reset() {
        idempotency.reset();
    }

    @Test
    void singlePacketDeliveredByThreeBridgesSettlesExactlyOnce() throws Exception {
        BigDecimal before = accounts.findByAccountName("alice").orElseThrow().getBalance();
        String ciphertext = crypto.encrypt(new PaymentInstruction(
                "alice", "bob", new BigDecimal("500.00"), "pin",
                UUID.randomUUID().toString(), Instant.now()));
        MeshPacket packet = new MeshPacket(UUID.randomUUID().toString(), 5, Instant.now(), ciphertext);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);
        Future<BridgeIngestionService.IngestionResult>[] deliveries = new Future[3];
        for (int i = 0; i < 3; i++) {
            deliveries[i] = executor.submit(() -> {
                start.await();
                return ingestion.ingest(packet);
            });
        }
        start.countDown();

        int settled = 0;
        int duplicates = 0;
        for (Future<BridgeIngestionService.IngestionResult> delivery : deliveries) {
            BridgeIngestionService.IngestionResult result = delivery.get(5, TimeUnit.SECONDS);
            if (result.outcome() == BridgeIngestionService.Outcome.SETTLED) settled++;
            if (result.outcome() == BridgeIngestionService.Outcome.DUPLICATE_DROPPED) duplicates++;
        }
        executor.shutdownNow();

        assertEquals(1, settled);
        assertEquals(2, duplicates);
        BigDecimal after = accounts.findByAccountName("alice").orElseThrow().getBalance();
        assertEquals(before.subtract(new BigDecimal("500.00")), after);
    }

    @Test
    void tamperedCiphertextIsInvalid() throws Exception {
        String ciphertext = crypto.encrypt(new PaymentInstruction(
                "alice", "bob", new BigDecimal("100.00"), "pin",
                UUID.randomUUID().toString(), Instant.now()));
        byte[] bytes = java.util.Base64.getDecoder().decode(ciphertext);
        bytes[bytes.length - 1] ^= 1;
        MeshPacket packet = new MeshPacket(UUID.randomUUID().toString(), 5, Instant.now(),
                java.util.Base64.getEncoder().encodeToString(bytes));

        BridgeIngestionService.IngestionResult result = ingestion.ingest(packet);

        assertEquals(BridgeIngestionService.Outcome.INVALID, result.outcome());
    }
}
