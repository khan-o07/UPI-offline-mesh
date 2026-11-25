package com.demo.upimesh.service;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.model.Account;
import com.demo.upimesh.model.AccountRepository;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.PaymentInstruction;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class DemoService {
    private final AccountRepository accounts;
    private final HybridCryptoService crypto;
    private final MeshSimulatorService mesh;

    public DemoService(AccountRepository accounts,
                       HybridCryptoService crypto,
                       MeshSimulatorService mesh) {
        this.accounts = accounts;
        this.crypto = crypto;
        this.mesh = mesh;
    }

    @PostConstruct
    public void seedAccounts() {
        if (accounts.count() == 0) {
            accounts.save(new Account("alice", new BigDecimal("5000.00")));
            accounts.save(new Account("bob", new BigDecimal("1000.00")));
            accounts.save(new Account("carol", new BigDecimal("750.00")));
            accounts.save(new Account("dave", new BigDecimal("1250.00")));
        }
    }

    public MeshPacket send(String sender, String receiver, BigDecimal amount, String pin) throws Exception {
        PaymentInstruction instruction = new PaymentInstruction(
                sender, receiver, amount, Integer.toString(pin.hashCode()), UUID.randomUUID().toString(), Instant.now());
        String ciphertext = crypto.encrypt(instruction);
        MeshPacket packet = mesh.createPacket(ciphertext);
        mesh.inject(packet);
        return packet;
    }
}
