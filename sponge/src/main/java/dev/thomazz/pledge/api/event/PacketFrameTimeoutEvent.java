package dev.thomazz.pledge.api.event;

import dev.thomazz.pledge.api.PacketFrame;
import dev.thomazz.pledge.api.Pledge;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;

/**
 * Called when a frame isn't received back within a certain amount of ticks.
 * The amount of ticks until timeout can be set using {@link Pledge#setTimeoutTicks(int)}
 */
@Getter
public class PacketFrameTimeoutEvent extends PacketFrameEvent {

    public PacketFrameTimeoutEvent(User player, PacketFrame frame) {
        super(player, frame);
    }
}
