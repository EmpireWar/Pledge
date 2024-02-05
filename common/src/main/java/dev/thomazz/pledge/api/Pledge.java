package dev.thomazz.pledge.api;

import dev.thomazz.pledge.util.MinecraftReflectionProvider;

import java.util.Optional;
import java.util.UUID;

/**
 * Main API instance for Pledge
 */
public interface Pledge<P, SP> {

    MinecraftReflectionProvider reflectionProvider();

    /**
     * Starts tracking packets and listening using the provided plugin instance.
     * <p>
     * @param plugin - Plugin instance to use
     */
    Pledge<P, SP> start(P plugin);

    /**
     * Ends and cleans up the API instance, clearing all old data.
     * This does not need to be called on shutdown, but should be used if support plugin reloading is desired.
     */
    void destroy();

    /**
     * Sets the range for the {@link PacketFrame} ids.
     * <p>
     * @param start - Starting id for range
     * @param end   - Ending id for range
     */
    Pledge<P, SP> setRange(int start, int end);

    /**
     * Sets the amount of ticks for a PacketFrameTimeoutEvent to be called,
     * after not receiving a response for a {@link PacketFrame}.
     * Default value is 400 ticks (20 seconds)
     * <p>
     * @param ticks - Amount of unresponsive ticks until calling the timeout event (<= 0 disables this feature)
     */
    Pledge<P, SP> setTimeoutTicks(int ticks);

    /**
     * Setting to send frames automatically after a certain amount of ticks have passed without any frames created.
     * Default value for this is 0, causing no frames to be created and sent automatically.
     * <p>
     * @param interval - Interval to automatically create packet frames for (<= 0 disables this feature)
     */
    Pledge<P, SP> setFrameInterval(int interval);

    /**
     * Tracks packets for the current tick, creating a new {@link PacketFrame}.
     * If a frame is already created for the player on this current tick, it simply returns the already existing frame.
     * <p>
     * Note: Players need to be in the PLAY state for creating frames, creating a frame before that will cause an error.
     * <p>
     * @param player - Player to create frame for
     * @return       - Created frame or current frame if one was already created this tick
     */
    PacketFrame getOrCreateFrame(SP player);

    /**
     * Same as {@link #getOrCreateFrame(Object)}, but instead using the player uuid.
     */
    PacketFrame getOrCreateFrame(UUID playerId);

    /**
     * Gets the {@link PacketFrame} for the player in the current server tick.
     * Returns an empty result if no {@link PacketFrame} was created with {@link #getOrCreateFrame(Object)}.
     * <p>
     * @param player - Player to get frame for
     * @return       - Next frame
     */
    Optional<PacketFrame> getFrame(SP player);

    /**
     * Same as {@link #getFrame(Object)}, but instead using the player uuid.
     */
    Optional<PacketFrame> getFrame(UUID playerId);

    /**
     * Forcefully calls the tick end method, flushing packets.
     */
    void forceFlushPackets(SP player);

    /**
     * @return - If the current server version supports packet bundles.
     */
    boolean supportsBundles();
}
