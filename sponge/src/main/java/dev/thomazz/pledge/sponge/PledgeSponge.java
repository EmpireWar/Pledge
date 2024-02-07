package dev.thomazz.pledge.sponge;

import com.google.inject.Inject;
import dev.thomazz.pledge.api.PacketFrame;
import dev.thomazz.pledge.api.Pledge;
import dev.thomazz.pledge.channel.ChannelAccess;
import dev.thomazz.pledge.channel.ReflectiveChannelAccess;
import dev.thomazz.pledge.packet.PacketBundleBuilder;
import dev.thomazz.pledge.packet.PacketProvider;
import dev.thomazz.pledge.packet.PacketProviderFactory;
import dev.thomazz.pledge.sponge.util.MinecraftReflection;
import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import dev.thomazz.pledge.util.TickEndTask;
import io.netty.channel.Channel;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.plugin.PluginContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class PledgeSponge implements Pledge<PluginContainer, ServerPlayer> {

    private final MinecraftReflectionProvider provider;
    private final ChannelAccess channelAccess;
    private final PacketBundleBuilder packetBundleBuilder;
    private final PacketProvider packetProvider;
    private final Map<UUID, PlayerHandlerImpl> playerHandlers;

    public PledgeSponge() {
        this.provider = new MinecraftReflection();
        this.channelAccess = new ReflectiveChannelAccess(this);
        this.packetBundleBuilder = new PacketBundleBuilder();
        this.packetProvider = PacketProviderFactory.build(this);
        this.playerHandlers = new HashMap<>();
    }

    /**
     * Sets up Pledge to start tracking packets using {@link PacketFrame}s.
     * Only a single instance should be active at a time to prevent undesirable behaviour.
     * <p>
     * @return - API instance
     */
    public static Pledge<PluginContainer, ServerPlayer> build() {
        return new PledgeSponge();
    }

    @Inject
    private Logger logger;

    @Inject
    private PluginContainer plugin;

    private ScheduledTask tickTask;
    private TickEndTask tickEndTask;
    private int tick;

    private int timeoutTicks = 400;
    private int frameInterval = 0;

    // Default values, can modify through API
    private int rangeStart = -2000;
    private int rangeEnd = -3000;

    // Determines if this instance is active or not
    private boolean started = false;
    private boolean destroyed = false;

    private void createHandler(User player) {
        this.createHandler(player, this.channelAccess.getChannel(player, player.uniqueId()));
    }

    private void createHandler(User player, Channel channel) {
        try {
            PlayerHandlerImpl handler = new PlayerHandlerImpl(this, player, channel);
            this.playerHandlers.put(player.uniqueId(), handler);
        } catch (Exception e) {
            logger.error("Could not create Pledge player handler!");
            e.printStackTrace();
        }
    }

    private void removeHandler(ServerPlayer player) {
        this.playerHandlers.remove(player.uniqueId());
    }

    private Optional<PlayerHandlerImpl> getHandler(ServerPlayer player) {
        return this.getHandler(player.uniqueId());
    }

    private Optional<PlayerHandlerImpl> getHandler(UUID playerId) {
        return Optional.ofNullable(this.playerHandlers.get(playerId));
    }

    private void validateActive() {
        if (this.destroyed) {
            throw new IllegalStateException("Pledge instance is no longer active because it has been destroyed!");
        }
    }

    private void validateBounds(int rangeId) {
        int min = this.packetProvider.getLowerBound();
        int max = this.packetProvider.getUpperBound();

        if (rangeId < min || rangeId > max) {
            throw new IllegalArgumentException("Invalid range for packet provider!"
                    + "limits: " + min + " - " + max);
        }
    }

    private void tickStart() {
        // Frame creation for all online players if a frame interval is set
        if (this.frameInterval > 0) {
            this.playerHandlers.values().stream()
                    .filter(PlayerHandlerImpl::isActive)
                    .filter(handler -> handler.getCreationTicks() >= this.frameInterval)
                    .forEach(PlayerHandlerImpl::createNextFrame);
        }

        // Tick player handlers
        this.playerHandlers.values().forEach(PlayerHandlerImpl::tickStart);
        this.tick++;
    }

    private void tickEnd() {
        this.playerHandlers.values().forEach(PlayerHandlerImpl::tickEnd);
    }

    @Override
    public void forceFlushPackets(ServerPlayer player) {
        getHandler(player).ifPresent(PlayerHandlerImpl::processTickEnd);
    }

    @Override
    public MinecraftReflectionProvider reflectionProvider() {
        return provider;
    }

    @Override
    public Pledge<PluginContainer, ServerPlayer> start(PluginContainer plugin) {
        this.validateActive();

        if (this.started) {
            throw new IllegalStateException("Already started Pledge instance!");
        }

        this.plugin = plugin;
        this.tickTask = Sponge.server().scheduler().submit(Task.builder().interval(Ticks.single()).execute(this::tickStart).build());
        TickEndTask.initialise(this);
        this.tickEndTask = new TickEndTask(this::tickEnd).start();

        Sponge.eventManager().registerListeners(plugin, this);
        logger.info("Started up Pledge");

        // Mainly for reload support or when starting later
        Sponge.server().onlinePlayers().forEach(player -> this.createHandler(player.user()));

        this.started = true;
        return this;
    }

    @Override
    public void destroy() {
        this.validateActive();

        // End tick task if it was created
        this.tickTask.cancel();
        this.tickEndTask.cancel();

        // Unregister listening for player join and quit
        Sponge.eventManager().unregisterListeners(this);

        // Clean up all player handlers
        this.playerHandlers.values().forEach(PlayerHandlerImpl::cleanUp);
        this.playerHandlers.clear();

        // Clear instance to allow creation of a new one
        this.destroyed = true;
    }

    @Override
    public Pledge<PluginContainer, ServerPlayer> setRange(int start, int end) {
        this.validateActive();
        this.validateBounds(start);
        this.validateBounds(end);

        this.rangeStart = start;
        this.rangeEnd = end;
        return this;
    }

    @Override
    public Pledge<PluginContainer, ServerPlayer> setTimeoutTicks(int ticks) {
        this.validateActive();
        this.timeoutTicks = ticks;
        return this;
    }

    @Override
    public Pledge<PluginContainer, ServerPlayer> setFrameInterval(int interval) {
        this.validateActive();
        this.frameInterval = interval;
        return this;
    }

    @Override
    public PacketFrame getOrCreateFrame(ServerPlayer player) {
        return this.getOrCreateFrame(player.uniqueId());
    }

    @Override
    public PacketFrame getOrCreateFrame(UUID playerId) {
        this.validateActive();
        return this.getHandler(playerId).map(PlayerHandlerImpl::createNextFrame)
                .orElseThrow(() -> new IllegalArgumentException("No handler present for player!"));
    }

    @Override
    public Optional<PacketFrame> getFrame(ServerPlayer player) {
        return this.getFrame(player.uniqueId());
    }

    @Override
    public Optional<PacketFrame> getFrame(UUID playerId) {
        this.validateActive();
        return this.getHandler(playerId).flatMap(PlayerHandlerImpl::getCurrentFrame);
    }

    @Override
    public boolean supportsBundles() {
        return this.packetBundleBuilder.isSupported();
    }

    // Lowest priority to have data be available on join event
    @Listener(order = Order.LAST)
    @IsCancelled(Tristate.FALSE) // No reason to create a handler if login isn't allowed
    public void onPlayerLogin(ServerSideConnectionEvent.Login event) {
        User player = event.user();
        this.createHandler(player, this.channelAccess.getChannel(player, player.uniqueId()));
    }

    // If for some reason we want this to be available on the quit event
    @Listener(order = Order.LAST)
    public void onPlayerQuit(ServerSideConnectionEvent.Disconnect event) {
        this.removeHandler(event.player());
    }
}
