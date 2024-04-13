package dev.thomazz.pledge.network.queue;

import com.google.common.collect.ImmutableList;
import dev.thomazz.pledge.Pledge;

import java.util.List;

public final class PacketFiltering {

    private final Pledge<?> pledge;
    private final List<Class<?>> queueWhiteListPackets;
    private final List<Class<?>> loginPackets;

    public PacketFiltering(Pledge<?> pledge) {
        this.pledge = pledge;

        this.queueWhiteListPackets = buildQueueWhitelistPackets();
        this.loginPackets = buildLoginPackets();

        if (queueWhiteListPackets.isEmpty()) {
            pledge.logger().warning("Failed to generate whitelist packets");
        }

        if (loginPackets.isEmpty()) {
            pledge.logger().warning("Failed to generate login packets");
        }
    }

    private List<Class<?>> buildQueueWhitelistPackets() {
        ImmutableList.Builder<Class<?>> builder = ImmutableList.builder();
        addGamePacket(builder, "PacketPlayOutKeepAlive");
        addGamePacket(builder, "ClientboundKeepAlivePacket");
        addGamePacket(builder, "PacketPlayOutKickDisconnect");
        addGamePacket(builder, "ClientboundDisconnectPacket");
        return builder.build();
    }

    private List<Class<?>> buildLoginPackets() {
        ImmutableList.Builder<Class<?>> builder = ImmutableList.builder();
        addGamePacket(builder, "PacketPlayOutLogin");
        addGamePacket(builder, "ClientboundLoginPacket");
        return builder.build();
    }

    private void addGamePacket(ImmutableList.Builder<Class<?>> builder, String packetName) {
        try {
            builder.add(pledge.getReflectionProvider().gamePacket(packetName));
        } catch (ReflectiveOperationException ignored) {
        }
    }

    // If a packet should be added to the packet queue or instantly sent to players
    public boolean isWhitelistedFromQueue(Object packet) {
        return queueWhiteListPackets.stream().anyMatch(type -> type.isInstance(packet));
    }

    // Login packets initiate the game start protocol
    public boolean isLoginPacket(Object packet) {
        return loginPackets.stream().anyMatch(type -> type.isInstance(packet));
    }
}
