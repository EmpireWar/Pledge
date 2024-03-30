package dev.thomazz.pledge.sponge.event;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * Called at the start of a server tick.
 */
public class TickStartEvent extends AbstractEvent {

    private final Cause cause;

    public TickStartEvent() {
        this.cause = Cause.of(EventContext.empty(), Sponge.server());
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
