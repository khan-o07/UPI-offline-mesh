package com.demo.upimesh.service;

import com.demo.upimesh.model.MeshPacket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class MeshSimulatorService {
    private final Map<String, VirtualDevice> devices = new LinkedHashMap<>();
    private final int defaultTtl;
    private final BridgeIngestionService ingestion;

    public MeshSimulatorService(@Value("${demo.packet.ttl:5}") int defaultTtl,
                                BridgeIngestionService ingestion) {
        this.defaultTtl = defaultTtl;
        this.ingestion = ingestion;
        seedDevices();
    }

    private void seedDevices() {
        devices.put("phone-alice", new VirtualDevice("phone-alice", false));
        devices.put("phone-bob", new VirtualDevice("phone-bob", false));
        devices.put("phone-carol", new VirtualDevice("phone-carol", false));
        devices.put("phone-dave", new VirtualDevice("phone-dave", false));
        devices.put("phone-bridge", new VirtualDevice("phone-bridge", true));
    }

    public void inject(MeshPacket packet) {
        devices.values().forEach(device -> {
            if (device.id().equals("phone-alice")) {
                device.store(packet);
            }
        });
    }

    public MeshPacket createPacket(String ciphertext) {
        return new MeshPacket(UUID.randomUUID().toString(), defaultTtl, Instant.now(), ciphertext);
    }

    public int gossipRound() {
        Collection<MeshPacket> carried = new ArrayList<>();
        for (VirtualDevice device : devices.values()) {
            for (MeshPacket packet : device.packets()) {
                if (packet.ttl() > 0) {
                    carried.add(new MeshPacket(packet.packetId(), packet.ttl() - 1,
                            packet.createdAt(), packet.ciphertext()));
                }
            }
        }
        int changed = 0;
        for (VirtualDevice source : devices.values()) {
            for (MeshPacket packet : carried) {
                int before = source.packetCount();
                source.store(packet);
                if (source.packetCount() > before) {
                    changed++;
                }
            }
        }
        return changed;
    }

    public List<BridgeIngestionService.IngestionResult> flushBridges() {
        List<CompletableFuture<BridgeIngestionService.IngestionResult>> uploads = new ArrayList<>();
        for (VirtualDevice device : devices.values()) {
            if (device.hasInternet()) {
                for (MeshPacket packet : device.packets()) {
                    uploads.add(CompletableFuture.supplyAsync(() -> ingestion.ingest(packet)));
                }
            }
        }
        return uploads.stream().map(CompletableFuture::join).toList();
    }

    public void reset() {
        devices.values().forEach(VirtualDevice::clear);
    }

    public List<Map<String, Object>> state() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (VirtualDevice device : devices.values()) {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("id", device.id());
            state.put("hasInternet", device.hasInternet());
            state.put("packetCount", device.packetCount());
            result.add(state);
        }
        return result;
    }

    public int packetCount() {
        return devices.values().stream().mapToInt(VirtualDevice::packetCount).sum();
    }
}
