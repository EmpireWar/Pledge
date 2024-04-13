package dev.thomazz.pledge.network;

import io.netty.channel.ChannelPromise;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NetworkMessage {
    private final Object message;
    private final Class<?> packetType;
    private final ChannelPromise promise;

    public static NetworkMessage of(Object message, Class<?> packetType, ChannelPromise promise) {
        return new NetworkMessage(message, packetType, promise);
    }
}