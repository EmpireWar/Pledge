package dev.thomazz.pledge.network.queue;

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
public class ChannelMessageQueueHandler extends ChannelOutboundHandlerAdapter {
    private final Deque<QueuedMessage> messageQueue = new ConcurrentLinkedDeque<>();
    private QueueMode mode = QueueMode.PASS;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        switch (this.mode) {
            case ADD_FIRST:
                this.messageQueue.addFirst(new QueuedMessage(msg, promise));
                break;
            case ADD_LAST:
                this.messageQueue.addLast(new QueuedMessage(msg, promise));
                break;
            default:
            case PASS:
                super.write(ctx, msg, promise);
                break;
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        this.drain(ctx, true);
        super.close(ctx, promise);
    }

    public void drain(ChannelHandlerContext ctx, boolean flush) {
        while (!this.messageQueue.isEmpty()) {
            QueuedMessage message = this.messageQueue.poll();
            ctx.write(message.message, message.promise);
        }

        if (flush) ctx.flush();
    }

    @RequiredArgsConstructor
    private static final class QueuedMessage {
        private final Object message;
        private final ChannelPromise promise;
    }
}
