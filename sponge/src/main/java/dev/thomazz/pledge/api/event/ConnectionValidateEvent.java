package dev.thomazz.pledge.api.event;

import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * An initial transaction to validate the connection is sent.
 * The main purpose of this is to synchronize the client connection with the server.
 * When using a forwarding proxy the client connection can still send packets intended for different servers.
 * After this event has been called, all responses from the client can be guaranteed to be intended for this server.
 */
@Getter
public class ConnectionValidateEvent extends AbstractEvent {

    private final User player;
    private final int validationId;
    private final Cause cause;

    public ConnectionValidateEvent(User player, int validationId) {
        this.player = player;
        this.validationId = validationId;
        this.cause = Cause.of(EventContext.empty(), player);
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
