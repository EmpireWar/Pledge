package dev.thomazz.pledge.pinger.frame;

import dev.thomazz.pledge.pinger.ClientPingerListener;
import dev.thomazz.pledge.pinger.frame.data.Frame;

import java.util.UUID;

/**
 * Listener that can be attached to a {@link FrameClientPinger} for events regarding {@link Frame} objects.
 */
public interface FrameClientPingerListener extends ClientPingerListener {
    /**
     * Called when a {@link Frame} is created.
     * <p>
     * @param player - Player frame was created for
     * @param frame  - Frame created
     */
    default void onFrameCreate(UUID player, Frame frame) {}

    /**
     * Called when a {@link Frame} is sent.
     * <p>
     * @param player - Player frame was sent for
     * @param frame  - Frame sent
     */
    default void onFrameSend(UUID player, Frame frame) {}

    /**
     * Called when a {@link Frame} start ID is received.
     * <p>
     * @param player - Player frame start ID was received for
     * @param frame  - Frame received
     */
    default void onFrameReceiveStart(UUID player, Frame frame) {}

    /**
     * Called when a {@link Frame} end ID is received.
     * <p>
     * @param player - Player frame end ID was received for
     * @param frame  - Frame received
     */
    default void onFrameReceiveEnd(UUID player, Frame frame) {}
}
