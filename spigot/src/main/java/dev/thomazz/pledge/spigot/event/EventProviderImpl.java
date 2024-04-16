package dev.thomazz.pledge.spigot.event;

import dev.thomazz.pledge.event.EventProvider;
import org.bukkit.Bukkit;

import java.util.UUID;

public class EventProviderImpl implements EventProvider {

    @Override
    public boolean callPongReceive(UUID player, int id) {
        final PongReceiveEvent event = new PongReceiveEvent(player, id);
        Bukkit.getPluginManager().callEvent(event);
        return event.isValidated();
    }
}
