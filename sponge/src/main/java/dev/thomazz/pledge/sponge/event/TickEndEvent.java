package dev.thomazz.pledge.sponge.event;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * Called at the end of a server tick.
 */
public class TickEndEvent extends AbstractEvent {

    private final Cause cause;

    public TickEndEvent() {
        this.cause = Cause.of(EventContext.empty(), Sponge.server());
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
