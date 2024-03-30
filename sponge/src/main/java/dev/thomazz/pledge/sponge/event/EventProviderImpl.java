package dev.thomazz.pledge.sponge.event;

import dev.thomazz.pledge.event.EventProvider;
import org.spongepowered.api.Sponge;

import java.util.UUID;

public class EventProviderImpl implements EventProvider {

    @Override
    public void callPongReceive(UUID player, int id) {
        Sponge.eventManager().post(new PongReceiveEvent(player, id));
    }
}
