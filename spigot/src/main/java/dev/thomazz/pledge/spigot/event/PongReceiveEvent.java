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
 * Called when a pong packet is received from a {@link Player}
 * Note: Executed from netty thread
 */
@Getter
@Setter
public class PongReceiveEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final UUID player;
    private final int id;
    private boolean cancelled = false;

    public PongReceiveEvent(UUID player, int id) {
        super(true);
        this.player = player;
        this.id = id;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return PongReceiveEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return PongReceiveEvent.handlers;
    }
}
