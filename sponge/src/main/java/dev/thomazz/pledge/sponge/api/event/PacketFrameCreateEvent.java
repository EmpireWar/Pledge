package dev.thomazz.pledge.sponge.api.event;

import dev.thomazz.pledge.api.PacketFrame;
import dev.thomazz.pledge.api.Pledge;
import org.spongepowered.api.entity.living.player.User;

import java.util.UUID;

/**
 * Called when a packet frame is created by {@link Pledge#getOrCreateFrame(UUID)}
 * Can also be created by the internal tick task specified with {@link Pledge#setFrameInterval(int)}
 */
public class PacketFrameCreateEvent extends PacketFrameEvent {

    public PacketFrameCreateEvent(User player, PacketFrame created) {
        super(player, created); // Event can be either on or off main thread
    }
}
