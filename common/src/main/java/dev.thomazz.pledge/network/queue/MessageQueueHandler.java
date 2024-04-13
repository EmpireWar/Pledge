package dev.thomazz.pledge.network.queue;

import dev.thomazz.pledge.network.NetworkMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

@Setter
@Getter
public class MessageQueueHandler extends ChannelOutboundHandlerAdapter {
    private final Deque<NetworkMessage> messageQueue = new ConcurrentLinkedDeque<>();
    private QueueMode mode = QueueMode.PASS;
    private boolean flushable;

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
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        if (!flushable) return;
        super.flush(ctx);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        this.drain(ctx, true);
        super.close(ctx, promise);
    }

    public void drain(ChannelHandlerContext ctx, boolean flush) {
        while (!this.messageQueue.isEmpty()) {
            NetworkMessage message = this.messageQueue.poll();
            ctx.write(message.getMessage(), message.getPromise());
        }

        if (flush) {
            flushable = true;
            ctx.flush();
            flushable = false;
        }
    }
}
