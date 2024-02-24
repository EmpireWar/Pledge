package dev.thomazz.pledge.spigot;

import dev.thomazz.pledge.api.PacketFrame;
import dev.thomazz.pledge.api.Pledge;
import dev.thomazz.pledge.channel.ChannelAccess;
import dev.thomazz.pledge.channel.ReflectiveChannelAccess;
import dev.thomazz.pledge.packet.PacketBundleBuilder;
import dev.thomazz.pledge.packet.PacketProvider;
import dev.thomazz.pledge.packet.PacketProviderFactory;
import dev.thomazz.pledge.spigot.util.MinecraftReflection;
import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import dev.thomazz.pledge.util.TickEndTask;
import io.netty.channel.Channel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class PledgeSpigot implements Pledge<JavaPlugin, Player>, Listener {

    private final MinecraftReflectionProvider provider;
    private final ChannelAccess channelAccess;
    private final PacketBundleBuilder packetBundleBuilder;
    private final PacketProvider packetProvider;
    private final Map<UUID, PlayerHandlerImpl> playerHandlers;

    public PledgeSpigot() {
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
    public static Pledge<JavaPlugin, Player> build() {
        return new PledgeSpigot();
    }

    private JavaPlugin plugin;

    private BukkitTask tickTask;
    private TickEndTask tickEndTask;
    private int tick;

    private int timeoutTicks = 400;
    private int frameInterval = 0;

    // Default values, can modify through API
    private boolean cancelPongs = false;
    private int rangeStart = -2000;
    private int rangeEnd = -3000;

    // Determines if this instance is active or not
    private boolean started = false;
    private boolean destroyed = false;

    private void createHandler(Player player) {
        this.createHandler(player, this.channelAccess.getChannel(player, player.getUniqueId()));
    }

    private void createHandler(Player player, Channel channel) {
        try {
            PlayerHandlerImpl handler = new PlayerHandlerImpl(this, player, channel);
            this.playerHandlers.put(player.getUniqueId(), handler);
        } catch (Exception e) {
            this.plugin.getLogger().severe("Could not create Pledge player handler!");
            e.printStackTrace();
        }
    }

    private void removeHandler(Player player) {
        this.playerHandlers.remove(player.getUniqueId());
    }

    private Optional<PlayerHandlerImpl> getHandler(Player player) {
        return this.getHandler(player.getUniqueId());
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
    public void finishFrame(Player player, boolean flush) {
        getHandler(player).ifPresent(handler -> handler.processTickEnd(flush));
    }

    @Override
    public MinecraftReflectionProvider reflectionProvider() {
        return provider;
    }

    @Override
    public Pledge<JavaPlugin, Player> start(JavaPlugin plugin) {
        this.validateActive();

        if (this.started) {
            throw new IllegalStateException("Already started Pledge instance!");
        }

        this.plugin = plugin;
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickStart, 0L, 1L);
        TickEndTask.initialise(this);
        this.tickEndTask = new TickEndTask(this::tickEnd).start();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Started up Pledge");

        // Mainly for reload support or when starting later
        Bukkit.getOnlinePlayers().forEach(this::createHandler);

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
        HandlerList.unregisterAll(this);

        // Clean up all player handlers
        this.playerHandlers.values().forEach(PlayerHandlerImpl::cleanUp);
        this.playerHandlers.clear();

        // Clear instance to allow creation of a new one
        this.destroyed = true;
    }

    @Override
    public Pledge<JavaPlugin, Player> setRange(int start, int end) {
        this.validateActive();
        this.validateBounds(start);
        this.validateBounds(end);

        this.rangeStart = start;
        this.rangeEnd = end;
        return this;
    }

    @Override
    public Pledge<JavaPlugin, Player> setTimeoutTicks(int ticks) {
        this.validateActive();
        this.timeoutTicks = ticks;
        return this;
    }

    @Override
    public Pledge<JavaPlugin, Player> setFrameInterval(int interval) {
        this.validateActive();
        this.frameInterval = interval;
        return this;
    }

    @Override
    public PacketFrame getOrCreateFrame(Player player) {
        return this.getOrCreateFrame(player.getUniqueId());
    }

    @Override
    public PacketFrame getOrCreateFrame(UUID playerId) {
        this.validateActive();
        return this.getHandler(playerId).map(PlayerHandlerImpl::createNextFrame)
                .orElseThrow(() -> new IllegalArgumentException("No handler present for player!"));
    }

    @Override
    public Optional<PacketFrame> getFrame(Player player) {
        return this.getFrame(player.getUniqueId());
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

    @Override
    public Pledge<JavaPlugin, Player> setCancelPongs(boolean cancelPongs) {
        this.cancelPongs = cancelPongs;
        return this;
    }

    @Override
    public boolean cancelPongs() {
        return cancelPongs;
    }

    // Lowest priority to have data be available on join event
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        // No reason to create a handler if login isn't allowed
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        Player player = event.getPlayer();
        this.createHandler(player, this.channelAccess.getChannel(player, player.getUniqueId()));
    }

    // If for some reason we want this to be available on the quit event
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.removeHandler(event.getPlayer());
    }
}
