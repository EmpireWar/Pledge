package dev.thomazz.pledge.sponge.api.event;

import dev.thomazz.pledge.api.PacketFrame;
import dev.thomazz.pledge.api.event.ErrorType;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;

/**
 * Called when an error is detected in the frame order.
 * For extra info see {@link ErrorType}
 * <p>
 * Note: This event is called from the netty event loop
 */
@Getter
public class PacketFrameErrorEvent extends PacketFrameEvent {

    private final ErrorType type;

    public PacketFrameErrorEvent(User player, PacketFrame frame, ErrorType type) {
        super(player, frame);
        this.type = type;
    }
}
