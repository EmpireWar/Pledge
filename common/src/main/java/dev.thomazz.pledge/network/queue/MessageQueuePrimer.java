package dev.thomazz.pledge.network.queue;

import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.packet.PacketBundleBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class MessageQueuePrimer extends ChannelOutboundHandlerAdapter {

    private final Pledge<?> pledge;
    private final MessageQueueHandler queueHandler;
    @Setter
    private Runnable endNextFrame;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Let whitelisted packets pass through the queue
        if (pledge.getPacketFilter().isWhitelistedFromQueue(msg) || pledge.getPacketFilter().isLoginPacket(msg)) {
            QueueMode lastMode = this.queueHandler.getMode();
            try {
                this.queueHandler.setMode(QueueMode.PASS);
                super.write(ctx, msg, promise);
            } finally {
                this.queueHandler.setMode(pledge.getPacketFilter().isLoginPacket(msg) ? QueueMode.ADD_LAST : lastMode);
                super.flush(ctx);
            }
            return;
        }

        try {
            super.write(ctx, msg, promise);
        } finally {
            // Support packets being added whilst writing a packet
            switch (this.queueHandler.getMode()) {
                case ADD_LAST:
                    this.queueHandler.getMessageQueue().peekLast().setPacketType(msg.getClass());
                    break;
                case ADD_FIRST:
                    this.queueHandler.getMessageQueue().peekFirst().setPacketType(msg.getClass());
                    break;
                case PASS:
                    break;
            }

            if (this.endNextFrame != null) {
                this.endNextFrame.run();
                this.endNextFrame = null;
            }
        }
    }
}
