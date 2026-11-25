package com.demo.upimesh.controller;

import com.demo.upimesh.crypto.ServerKeyHolder;
import com.demo.upimesh.model.Account;
import com.demo.upimesh.model.AccountRepository;
import com.demo.upimesh.model.MeshPacket;
import com.demo.upimesh.model.Transaction;
import com.demo.upimesh.model.TransactionRepository;
import com.demo.upimesh.service.BridgeIngestionService;
import com.demo.upimesh.service.DemoService;
import com.demo.upimesh.service.IdempotencyService;
import com.demo.upimesh.service.MeshSimulatorService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final ServerKeyHolder keys;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final DemoService demo;
    private final MeshSimulatorService mesh;
    private final IdempotencyService idempotency;
    private final BridgeIngestionService ingestion;

    public ApiController(ServerKeyHolder keys,
                         AccountRepository accounts,
                         TransactionRepository transactions,
                         DemoService demo,
                         MeshSimulatorService mesh,
                         IdempotencyService idempotency,
                         BridgeIngestionService ingestion) {
        this.keys = keys;
        this.accounts = accounts;
        this.transactions = transactions;
        this.demo = demo;
        this.mesh = mesh;
        this.idempotency = idempotency;
        this.ingestion = ingestion;
    }

    public record SendRequest(String sender, String receiver, BigDecimal amount, String pin) {}

    @GetMapping("/server-key")
    public Map<String, String> serverKey() {
        return Map.of("publicKey", keys.getPublicKeyBase64());
    }

    @GetMapping("/accounts")
    public List<Account> getAccounts() {
        return accounts.findAll();
    }

    @GetMapping("/transactions")
    public List<Transaction> getTransactions() {
        return transactions.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream().limit(20).toList();
    }

    @PostMapping("/demo/send")
    public ResponseEntity<?> send(@RequestBody SendRequest request) throws Exception {
        MeshPacket packet = demo.send(request.sender(), request.receiver(), request.amount(), request.pin());
        return ResponseEntity.ok(Map.of(
                "packetId", packet.packetId(),
                "ttl", packet.ttl(),
                "ciphertextLength", packet.ciphertext().length()
        ));
    }

    @PostMapping("/mesh/gossip")
    public Map<String, Object> gossip() {
        return Map.of("newDeliveries", mesh.gossipRound(), "mesh", mesh.state());
    }

    @GetMapping("/mesh/state")
    public List<Map<String, Object>> meshState() {
        return mesh.state();
    }

    @PostMapping("/mesh/flush")
    public Map<String, Object> flush() {
        return Map.of("uploads", mesh.flushBridges(), "mesh", mesh.state());
    }

    @PostMapping("/mesh/flushBridges")
    public Map<String, Object> flushBridges() {
        return flush();
    }

    @PostMapping("/mesh/reset")
    public Map<String, Object> reset() {
        mesh.reset();
        idempotency.reset();
        return Map.of("reset", true, "mesh", mesh.state());
    }

    @PostMapping("/bridge/ingest")
    public BridgeIngestionService.IngestionResult ingest(
            @RequestBody MeshPacket packet,
            @RequestHeader(value = "X-Bridge-Node-Id", required = false) String bridgeId,
            @RequestHeader(value = "X-Hop-Count", required = false) Integer hopCount) {
        return ingestion.ingest(packet);
    }
}
