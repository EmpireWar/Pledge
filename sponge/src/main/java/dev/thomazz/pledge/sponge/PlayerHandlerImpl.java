package dev.thomazz.pledge.sponge;

import dev.thomazz.pledge.PlayerHandler;
import dev.thomazz.pledge.api.PacketFrame;
import dev.thomazz.pledge.sponge.api.event.ConnectionValidateEvent;
import dev.thomazz.pledge.sponge.api.event.PacketFrameCreateEvent;
import dev.thomazz.pledge.sponge.api.event.PacketFrameErrorEvent;
import dev.thomazz.pledge.sponge.api.event.PacketFrameReceiveEvent;
import dev.thomazz.pledge.sponge.api.event.PacketFrameTimeoutEvent;
import dev.thomazz.pledge.sponge.network.PacketFrameOutboundHeadHandler;
import dev.thomazz.pledge.sponge.network.PacketFrameOutboundTailHandler;
import dev.thomazz.pledge.api.event.ErrorType;
import dev.thomazz.pledge.api.event.ReceiveType;
import dev.thomazz.pledge.network.PacketFrameInboundHandler;
import dev.thomazz.pledge.packet.PacketProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerHandlerImpl implements PlayerHandler {
    private final Queue<PacketFrame> frameQueue = new ConcurrentLinkedQueue<>();
    private final PledgeSponge pledge;

    @Getter
    private final User player;
    private final Channel channel;
    private final PacketFrameOutboundTailHandler tailHandler;

    @Getter
    private final int rangeStart;
    private final int rangeEnd;

    private final AtomicReference<PacketFrame> currentFrame = new AtomicReference<>();
    private final AtomicReference<PacketFrame> receivingFrame = new AtomicReference<>();
    private final AtomicInteger id;

    private final AtomicInteger waitingTicks = new AtomicInteger();
    private final AtomicInteger creationTicks = new AtomicInteger();

    private volatile boolean timedOut;
    private volatile boolean active;
    private volatile boolean validated;

    public PlayerHandlerImpl(PledgeSponge pledge, User player, Channel channel) {
        this.pledge = pledge;
        this.player = player;
        this.channel = channel;

        this.id = new AtomicInteger(this.rangeStart = pledge.getRangeStart());
        this.rangeEnd = pledge.getRangeEnd();

        // Create new channel handlers
        PacketProvider provider = pledge.getPacketProvider();
        PacketFrameInboundHandler inbound = new PacketFrameInboundHandler(this, provider);

        this.tailHandler = new PacketFrameOutboundTailHandler(pledge, this);
        PacketFrameOutboundHeadHandler headHandler = new PacketFrameOutboundHeadHandler(pledge, this, this.tailHandler);

        // We want to be right after the encoder and decoder so there's no interference with other packet listeners
        Runnable runnable = () -> {
            this.channel.pipeline().addAfter("decoder", PacketFrameInboundHandler.HANDLER_NAME, inbound);
            this.channel.pipeline().addAfter("prepender", PacketFrameOutboundTailHandler.HANDLER_NAME, this.tailHandler);
            this.channel.pipeline().addAfter("encoder", PacketFrameOutboundHeadHandler.HANDLER_NAME, headHandler);
        };

        // Check if in event loop
        if (this.channel.eventLoop().inEventLoop()) {
            runnable.run();
        } else {
            this.channel.eventLoop().execute(runnable);
        }
    }

    private int getAndUpdateId() {
        int previous = this.id.get();

        int result = this.id.addAndGet(Integer.compare(this.rangeEnd - this.rangeStart, 0));
        if (this.rangeEnd > this.rangeStart ? result > this.rangeEnd : result < this.rangeEnd) {
            this.id.set(this.rangeStart);
        }

        return previous;
    }

    private void callEvent(AbstractEvent event) {
        Sponge.eventManager().post(event);
    }

    private void resetWaitTicks() {
        this.waitingTicks.set(0);
        this.timedOut = false;
    }

    public void tickStart() {
        // Only increment wait ticks when actually waiting for a frame, otherwise we can just reset wait ticks
        PacketFrame waiting = this.frameQueue.peek();
        if (waiting != null) {
            // Make sure that we don't spam call the event and wait for the next reset
            if (this.waitingTicks.incrementAndGet() > this.pledge.getTimeoutTicks() && !this.timedOut) {
                this.callEvent(new PacketFrameTimeoutEvent(this.player, waiting));
                this.timedOut = true;
            }
        } else {
            this.resetWaitTicks();
        }

        // Increment ticks since last frame was created
        this.creationTicks.incrementAndGet();
    }

    public void tickEnd() {
        this.channel.eventLoop().execute(this::processTickEnd);
    }

    public void processTickEnd() {
        // Make sure to offer the next frame to the handler and the awaiting frame queue
        PacketFrame frame = this.currentFrame.getAndSet(null);
        if (frame != null) {
            this.frameQueue.offer(frame);
        }

        // Drain net handler since flushing is overridden
        ChannelHandlerContext context = this.channel.pipeline().context(this.tailHandler);

        // Context could be null if the channel already had the net handler removed
        if (context != null) {
            try {
                this.tailHandler.drain(context, frame);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    // Processes incoming ids from netty thread
    @Override
    public void processId(int id) {
        // Make sure the ID is within the range
        if (id < Math.min(this.rangeStart, this.rangeEnd) || id > Math.max(this.rangeStart, this.rangeEnd)) {
            return;
        }

        // If awaiting connection validation
        if (!this.validated && id == this.rangeStart) {
            this.callEvent(new ConnectionValidateEvent(this.player, id));
            this.validated = true;
            return;
        }

        // Handle sent frames
        PacketFrame receiving = this.receivingFrame.get();
        if (receiving == null) {
            PacketFrame frame = this.frameQueue.peek();
            if (frame != null && frame.getId1() == id) {
                this.receivingFrame.set(this.frameQueue.poll());
                this.callEvent(new PacketFrameReceiveEvent(this.player, frame, ReceiveType.RECEIVE_START));
            } else {
                this.callEvent(new PacketFrameErrorEvent(this.player, frame, ErrorType.MISSING_FRAME));
            }
        } else {
            if (receiving.getId2() == id) {
                this.callEvent(new PacketFrameReceiveEvent(this.player, receiving, ReceiveType.RECEIVE_END));
                this.receivingFrame.set(null);
            } else {
                this.callEvent(new PacketFrameErrorEvent(this.player, receiving, ErrorType.INCOMPLETE_FRAME));
            }
        }

        // Reset waiting ticks because we received a correct response
        this.resetWaitTicks();
    }

    public Optional<PacketFrame> getCurrentFrame() {
        return Optional.ofNullable(this.currentFrame.get());
    }

    // Creates a new frame for the current tick if there is not already one
    public PacketFrame createNextFrame() {
        if (!this.active) {
            throw new IllegalStateException("Handler has not been activated yet!");
        }

        PacketFrame frame = this.currentFrame.get();
        if (frame == null) {
            this.currentFrame.set(frame = new PacketFrame(this.getAndUpdateId(), this.getAndUpdateId()));
            this.callEvent(new PacketFrameCreateEvent(this.player, frame));
        }

        // Reset creation ticks
        this.creationTicks.set(0);
        return frame;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getCreationTicks() {
        return this.creationTicks.get();
    }

    public void cleanUp() {
        try {
            // Try to remove the channel handlers
            this.channel.pipeline().remove("pledge_frame_outbound");
            this.channel.pipeline().remove("pledge_frame_inbound");
        } catch (NoSuchElementException ignored) {
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
