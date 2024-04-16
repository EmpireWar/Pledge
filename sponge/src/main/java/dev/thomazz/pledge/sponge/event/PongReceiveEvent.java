package dev.thomazz.pledge.sponge.event;

import lombok.Getter;
import lombok.Setter;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.UUID;

/**
 * Called when a pong packet is received from a {@link org.spongepowered.api.entity.living.player.server.ServerPlayer}
 * Note: Executed from netty thread
 */
@Getter
@Setter
public class PongReceiveEvent extends AbstractEvent implements Cancellable {

    private final UUID player;
    private final int id;
    private boolean isValidated;
    private boolean cancelled = false;
    private final Cause cause;

    public PongReceiveEvent(UUID player, int id) {
        this.player = player;
        this.id = id;
        this.cause = Cause.of(EventContext.empty(), player);
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
