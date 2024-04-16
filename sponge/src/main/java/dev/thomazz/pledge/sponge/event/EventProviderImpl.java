package dev.thomazz.pledge.sponge.event;

import dev.thomazz.pledge.event.EventProvider;
import org.spongepowered.api.Sponge;

import java.util.UUID;

public class EventProviderImpl implements EventProvider {

    @Override
    public boolean callPongReceive(UUID player, int id) {
        final PongReceiveEvent event = new PongReceiveEvent(player, id);
        Sponge.eventManager().post(event);
        return event.isValidated();
    }
}
