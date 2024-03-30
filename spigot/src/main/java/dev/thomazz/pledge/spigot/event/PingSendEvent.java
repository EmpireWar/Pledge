package dev.thomazz.pledge.spigot.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Called when a ping packet is sent to a {@link Player}
 * Note: Executed from netty thread
 */
@Getter
@Setter
public class PingSendEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final UUID player;
    private final int id;
    private boolean cancelled = false;

    public PingSendEvent(UUID player, int id) {
        super(true);
        this.player = player;
        this.id = id;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return PingSendEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return PingSendEvent.handlers;
    }
}
