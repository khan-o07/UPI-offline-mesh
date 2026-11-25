package com.demo.upimesh;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.model.PaymentInstruction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HybridCryptoTest {
    @Autowired
    private HybridCryptoService crypto;

    @Test
    void encryptDecryptRoundTrip() throws Exception {
        PaymentInstruction original = new PaymentInstruction(
                "alice", "bob", new BigDecimal("500.00"), "pin", UUID.randomUUID().toString(), Instant.now());

        PaymentInstruction decrypted = crypto.decrypt(crypto.encrypt(original));

        assertEquals(original.sender(), decrypted.sender());
        assertEquals(original.receiver(), decrypted.receiver());
        assertEquals(original.amount(), decrypted.amount());
        assertEquals(original.nonce(), decrypted.nonce());
    }

    @Test
    void tamperedCiphertextIsRejected() throws Exception {
        String ciphertext = crypto.encrypt(new PaymentInstruction(
                "alice", "bob", new BigDecimal("500.00"), "pin",
                UUID.randomUUID().toString(), Instant.now()));

        byte[] bytes = java.util.Base64.getDecoder().decode(ciphertext);
        bytes[bytes.length - 1] ^= 1;
        String tampered = java.util.Base64.getEncoder().encodeToString(bytes);

        assertThrows(Exception.class, () -> crypto.decrypt(tampered));
    }
}
