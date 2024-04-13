package dev.thomazz.pledge;

import dev.thomazz.pledge.event.EventProvider;
import dev.thomazz.pledge.network.queue.PacketFiltering;
import dev.thomazz.pledge.packet.PacketBundleBuilder;
import dev.thomazz.pledge.packet.PingPacketProvider;
import dev.thomazz.pledge.pinger.ClientPinger;
import dev.thomazz.pledge.pinger.frame.FrameClientPinger;
import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Main API object
 */
public interface Pledge<SP> {

    default boolean supportsBundles() {
        return PacketBundleBuilder.INSTANCE.isSupported();
    }

    /**
     * Sends a player a ping packet with a certain ID.
     * Can listen to events after sending the ping.
     * <p>
     * @param player - Player to send ping
     * @param id     - ID of the ping
     */
    default void sendPing(@NotNull SP player, int id) {
        sendPing(asUUID(player), id);
    }

    void sendPing(@NotNull UUID player, int id);

    void sendPingRaw(@NotNull UUID player, @NotNull Channel channel, int pingId);

    /**
     * Gets the networking channel for a {@link SP} if available.
     * <p>
     * @param player - Player to get channel for
     * @return       - Networking channel
     */
    Optional<Channel> getChannel(@NotNull UUID player);

    /**
     * Creates a client pinger.
     * See documentation in {@link ClientPinger} for more info.
     * <p>
     * @param startId - Start ID for ping range
     * @param endId   - End ID for ping range
     * @return        - Client pinger instance
     */
    ClientPinger<SP> createPinger(int startId, int endId);

    /**
     * Creates a frame client pinger.
     * See documentation in {@link FrameClientPinger} for more info.
     * <p>
     * @param startId - Start ID for ping range
     * @param endId   - End ID for ping range
     * @return        - Frame client pinger instance
     */
    FrameClientPinger<SP> createFramePinger(int startId, int endId);

    PingPacketProvider getPacketProvider();

    Logger logger();

    /**
     * Destroys the API instance.
     * A new API instance can be retrieved and created using Pledge's server implementation with #getOrCreate(Plugin).
     */
    void destroy();

    EventProvider eventProvider();

    MinecraftReflectionProvider getReflectionProvider();

    PacketFiltering getPacketFilter();

    UUID asUUID(SP player);

}
