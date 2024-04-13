package dev.thomazz.pledge.network.queue;

import dev.thomazz.pledge.Pledge;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageQueuePrimer extends ChannelOutboundHandlerAdapter {

    private final Pledge<?> pledge;
    private final MessageQueueHandler queueHandler;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Let whitelisted packets pass through the queue
        if (pledge.getPacketFilter().isWhitelistedFromQueue(msg) || pledge.getPacketFilter().isLoginPacket(msg)) {
            QueueMode lastMode = this.queueHandler.getMode();
            try {
                this.queueHandler.setMode(QueueMode.PASS);
                super.write(ctx, msg, promise);
            } finally {
                this.queueHandler.setMode(lastMode);
                super.flush(ctx);
            }
            return;
        }

        this.queueHandler.setNextPacketType(msg.getClass());
        super.write(ctx, msg, promise);
    }

}
