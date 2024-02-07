package dev.thomazz.pledge.sponge.api.event;

import io.netty.channel.Channel;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * Called when the play login packet is sent to the player from the server.
 * This indicates the start of the play game state and activates the player handler.
 */
@Getter
public class ActivateHandlerEvent extends AbstractEvent {

    private final User player;
    private final Channel channel;
    private final Cause cause;

    public ActivateHandlerEvent(User player, Channel channel) {
        this.player = player;
        this.channel = channel;
        this.cause = Cause.of(EventContext.empty(), player);
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
