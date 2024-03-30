package dev.thomazz.pledge.pinger;

import java.util.UUID;

/**
 * Listener to attach to a {@link ClientPinger}
 */
public interface ClientPingerListener {
    /**
     * Called when a player receives the first transaction ID of the {@link ClientPinger}.
     * After this the player can be considered active on the server.
     * <p>
     * @param player - Player that the ping response is received from
     * @param id     - ID of ping
     */
    default void onValidation(UUID player, int id) {}

    /**
     * Called when a ping is sent at the start of the tick to a player.
     * <p>
     * @param player - Player that the ping response is sent to
     * @param id     - ID of ping
     */
    default void onPingSendStart(UUID player, int id) {}

    /**
     * Called when a ping is sent at the end of the tick to a player.
     * <p>
     * @param player - Player that the ping response is sent to
     * @param id     - ID of ping
     */
    default void onPingSendEnd(UUID player, int id) {}

    /**
     * Called when the response to a ping that was sent at the start of the tick to a player is received.
     * <p>
     * @param player - Player that the ping response is received from
     * @param id     - ID of ping
     */
    default void onPongReceiveStart(UUID player, int id) {}

    /**
     * Called when the response to a ping that was sent at the end of the tick to a player is received.
     * <p>
     * @param player - Player that the ping response is received from
     * @param id     - ID of ping
     */
    default void onPongReceiveEnd(UUID player, int id) {}
}
