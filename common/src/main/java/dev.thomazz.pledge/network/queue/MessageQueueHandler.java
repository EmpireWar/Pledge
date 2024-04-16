package dev.thomazz.pledge.network.queue;

import dev.thomazz.pledge.network.NetworkMessage;
import dev.thomazz.pledge.packet.PacketBundleBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import lombok.Setter;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

@Setter
@Getter
public class MessageQueueHandler extends ChannelOutboundHandlerAdapter {

    private final Deque<NetworkMessage> messageQueue = new ConcurrentLinkedDeque<>();
    private QueueMode mode = QueueMode.PASS;
    private Runnable endNextFrame;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        switch (this.mode) {
            case ADD_FIRST:
                this.messageQueue.addFirst(NetworkMessage.of(msg, promise));
                break;
            case ADD_LAST:
                this.messageQueue.addLast(NetworkMessage.of(msg, promise));
                break;
            default:
            case PASS:
                super.write(ctx, msg, promise);
                break;
        }

        if (this.endNextFrame != null) {
            this.endNextFrame.run();
            this.endNextFrame = null;
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        this.drain(ctx, true);
        super.close(ctx, promise);
    }

    public void stripBundles() {
        // Packet type can be null if a plugin in the pipeline added their own packets - no good way to handle this.
        messageQueue.removeIf(msg -> msg.getPacketType() != null && PacketBundleBuilder.INSTANCE.isDelimiter(msg.getPacketType()));
    }

    public void drain(ChannelHandlerContext ctx, boolean flush) {
        while (!this.messageQueue.isEmpty()) {
            NetworkMessage message = this.messageQueue.poll();
            ctx.write(message.getMessage(), message.getPromise());
        }

        if (flush && ctx.channel().isOpen()) ctx.flush();
    }
}
