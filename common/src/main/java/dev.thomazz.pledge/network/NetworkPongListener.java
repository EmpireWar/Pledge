package dev.thomazz.pledge.network;

import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.packet.PingPacketProvider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class NetworkPongListener extends ChannelInboundHandlerAdapter {

    private final Pledge<?> clientPing;
    private final UUID player;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PingPacketProvider packetProvider = this.clientPing.getPacketProvider();

        if (packetProvider.isPong(msg)) {
            int id = packetProvider.idFromPong(msg);
            final boolean validated = clientPing.eventProvider().callPongReceive(player, id);
            if (validated && clientPing.cancelPongs()) {
                return;
            }
        }

        super.channelRead(ctx, msg);
    }
}
