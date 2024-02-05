package dev.thomazz.pledge.api.event;

import dev.thomazz.pledge.api.PacketFrame;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

@Getter
public abstract class PacketFrameEvent extends AbstractEvent {

    private final User player;
    private final PacketFrame frame;
    private final Cause cause;

    protected PacketFrameEvent(User player, PacketFrame frame) {
        this.player = player;
        this.frame = frame;
        this.cause = Cause.of(EventContext.empty(), player);
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
