package com.demo.upimesh.service;

import com.demo.upimesh.model.MeshPacket;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VirtualDevice {
    private final String id;
    private final boolean hasInternet;
    private final Map<String, MeshPacket> packets = new LinkedHashMap<>();

    public VirtualDevice(String id, boolean hasInternet) {
        this.id = id;
        this.hasInternet = hasInternet;
    }

    public synchronized void store(MeshPacket packet) {
        packets.putIfAbsent(packet.packetId(), packet);
    }

    public synchronized void clear() {
        packets.clear();
    }

    public synchronized Collection<MeshPacket> packets() {
        return List.copyOf(packets.values());
    }

    public String id() { return id; }
    public boolean hasInternet() { return hasInternet; }
    public synchronized int packetCount() { return packets.size(); }
}
