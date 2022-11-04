package dev.thomazz.pledge.packet;

public interface PacketProvider {
    Object buildPacket(int id) throws Exception;

    Integer idFromPacket(Object packet) throws Exception;

    boolean isKeepAlive(Object packet);

    boolean isDisconnect(Object packet);
}
