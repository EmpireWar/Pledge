package dev.thomazz.pledge.network.queue;

import com.google.common.collect.ImmutableList;
import dev.thomazz.pledge.Pledge;

import java.util.List;

public final class PacketQueueWhitelist {

    private final Pledge<?> pledge;
    private final List<Class<?>> whitelistedPackets = buildWhitelistedPackets();

    public PacketQueueWhitelist(Pledge<?> pledge) {
        this.pledge = pledge;
    }

    private List<Class<?>> buildWhitelistedPackets() {
        ImmutableList.Builder<Class<?>> builder = ImmutableList.builder();
        addGamePacket(builder, "PacketPlayOutKeepAlive");
        addGamePacket(builder, "ClientboundKeepAlivePacket");
        addGamePacket(builder, "PacketPlayOutKickDisconnect");
        addGamePacket(builder, "ClientboundDisconnectPacket");
        return builder.build();
    }

    private void addGamePacket(ImmutableList.Builder<Class<?>> builder, String packetName) {
        try {
            builder.add(pledge.getReflectionProvider().gamePacket(packetName));
        } catch (Exception ignored) {
        }
    }

    // If a packet should be added to the packet queue or instantly sent to players
    public boolean isWhitelisted(Object packet) {
        return whitelistedPackets.stream().anyMatch(type -> type.isInstance(packet));
    }
}
