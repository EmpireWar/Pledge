package dev.thomazz.pledge.api.event;

import dev.thomazz.pledge.api.PacketFrame;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;

/**
 * Called when receiving a response from the client corresponding to a {@link PacketFrame}
 * For extra info see {@link ReceiveType}
 * <p>
 * Note: This event is called from the netty event loop
 */
@Getter
public class PacketFrameReceiveEvent extends PacketFrameEvent {

    private final ReceiveType type;

    public PacketFrameReceiveEvent(User player, PacketFrame frame, ReceiveType type) {
        super(player, frame);
        this.type = type;
    }
}
