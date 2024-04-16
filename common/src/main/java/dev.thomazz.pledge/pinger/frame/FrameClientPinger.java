package dev.thomazz.pledge.pinger.frame;

import dev.thomazz.pledge.pinger.ClientPinger;
import dev.thomazz.pledge.pinger.frame.data.Frame;

import java.util.UUID;

/**
 * Implementation of a {@link ClientPinger} with extra functionality to determine for each tick if pings should be sent.
 * <p>
 * If a frame is created using {@link #getOrCreate(UUID)},
 * all packets for the current server tick will have a ping sent before and after them.
 */
public interface FrameClientPinger<SP> extends ClientPinger<SP> {
    /**
     * Creates a frame, scheduling pings to be sent before and after all packets in the current server tick.
     * <p>
     * @param player - Player to create frame for
     * @return       - IDs of pings sent before {@link Frame#getStartId()} and after packets {@link Frame#getEndId()}
     */
    Frame getOrCreate(UUID player);

    /**
     * Finishes the current frame for the player.
     * @param player - Player to end frame for
     */
    void finishFrame(UUID player);

    /**
     * Schedules a frame to finish when the next packet is sent.
     * @param player - Player to end frame for
     */
    void scheduleFinishFrame(UUID player);

    /**
     * Attaches a listener to listen to any events for {@link Frame} objects.
     * <p>
     * @param listener - Listener to attach
     */
    void attach(FrameClientPingerListener listener);
}
