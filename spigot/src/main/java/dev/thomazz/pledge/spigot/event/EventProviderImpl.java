package dev.thomazz.pledge.spigot.event;

import dev.thomazz.pledge.event.EventProvider;
import org.bukkit.Bukkit;

import java.util.UUID;

public class EventProviderImpl implements EventProvider {

    @Override
    public void callPongReceive(UUID player, int id) {
        Bukkit.getPluginManager().callEvent(new PongReceiveEvent(player, id));
    }
}
