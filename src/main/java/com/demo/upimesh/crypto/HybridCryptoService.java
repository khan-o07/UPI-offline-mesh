package com.demo.upimesh.crypto;

import com.demo.upimesh.model.PaymentInstruction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class HybridCryptoService {
    private static final int AES_KEY_BITS = 256;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int RSA_WRAPPED_KEY_BYTES = 256;

    private final ServerKeyHolder keys;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public HybridCryptoService(ServerKeyHolder keys) {
        this.keys = keys;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String encrypt(PaymentInstruction instruction) throws Exception {
        byte[] plaintext = objectMapper.writeValueAsBytes(instruction);

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_BITS, secureRandom);
        SecretKey aesKey = keyGenerator.generateKey();

        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] aesCiphertext = aesCipher.doFinal(plaintext);

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, keys.getPublicKey());
        byte[] wrappedKey = rsaCipher.doFinal(aesKey.getEncoded());

        ByteBuffer envelope = ByteBuffer.allocate(wrappedKey.length + iv.length + aesCiphertext.length);
        envelope.put(wrappedKey).put(iv).put(aesCiphertext);
        return Base64.getEncoder().encodeToString(envelope.array());
    }

    public PaymentInstruction decrypt(String encryptedBase64) throws Exception {
        byte[] envelope = Base64.getDecoder().decode(encryptedBase64);
        if (envelope.length < RSA_WRAPPED_KEY_BYTES + GCM_IV_BYTES + 16) {
            throw new IllegalArgumentException("Ciphertext is too short");
        }

        byte[] wrappedKey = new byte[RSA_WRAPPED_KEY_BYTES];
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] aesCiphertext = new byte[envelope.length - RSA_WRAPPED_KEY_BYTES - GCM_IV_BYTES];

        ByteBuffer.wrap(envelope).get(wrappedKey).get(iv).get(aesCiphertext);

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.DECRYPT_MODE, keys.getPrivateKey());
        byte[] aesKeyBytes = rsaCipher.doFinal(wrappedKey);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] plaintext = aesCipher.doFinal(aesCiphertext);
        return objectMapper.readValue(plaintext, PaymentInstruction.class);
    }

    public String ciphertextHash(String encryptedBase64) throws Exception {
        byte[] ciphertext = Base64.getDecoder().decode(encryptedBase64);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(ciphertext);
        StringBuilder hash = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hash.append(String.format("%02x", value));
        }
        return hash.toString();
    }
}
