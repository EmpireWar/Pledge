package dev.thomazz.pledge.sponge.api.event;

import dev.thomazz.pledge.network.QueuedMessage;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.Queue;

/**
 * Called right before all packets are flushed through the pipeline in the packet frame handler.
 * Allows you to modify the packets sent or to track certain packets right before flushing them.
 */
@Getter
public class PacketFlushEvent extends AbstractEvent {

    private final User player;
    private final Queue<QueuedMessage> packets;
    private final Cause cause;

    public PacketFlushEvent(User player, Queue<QueuedMessage> packets) {
        this.player = player;
        this.packets = packets;
        this.cause = Cause.of(EventContext.empty(), player);
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
