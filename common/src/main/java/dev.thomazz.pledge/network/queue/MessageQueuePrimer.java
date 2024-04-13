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
            this.queueHandler.setMode(QueueMode.PASS);
            try {
                super.write(ctx, msg, promise);
                super.flush(ctx);
            } finally {
                this.queueHandler.setMode(lastMode);
            }
            return;
        }

        this.queueHandler.setNextPacketType(msg.getClass());
        super.write(ctx, msg, promise);
    }

}
