package dev.thomazz.pledge.sponge.api.event;

import dev.thomazz.pledge.api.PacketFrame;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;

/**
 * Called right before when a packet frame is sent to the player client.
 * <p>
 * Note: This event is called from the netty event loop
 */
@Getter
public class PacketFrameSendEvent extends PacketFrameEvent {

    public PacketFrameSendEvent(User player, PacketFrame current) {
        super(player, current);
    }

}
