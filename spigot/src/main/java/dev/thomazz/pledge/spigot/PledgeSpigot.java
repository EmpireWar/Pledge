package dev.thomazz.pledge.spigot;

import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.event.EventProvider;
import dev.thomazz.pledge.network.NetworkPongListener;
import dev.thomazz.pledge.network.queue.PacketFiltering;
import dev.thomazz.pledge.packet.PacketProviderFactory;
import dev.thomazz.pledge.packet.PingPacketProvider;
import dev.thomazz.pledge.pinger.ClientPinger;
import dev.thomazz.pledge.pinger.ClientPingerImpl;
import dev.thomazz.pledge.pinger.frame.FrameClientPinger;
import dev.thomazz.pledge.pinger.frame.FrameClientPingerImpl;
import dev.thomazz.pledge.spigot.event.EventProviderImpl;
import dev.thomazz.pledge.spigot.event.PingSendEvent;
import dev.thomazz.pledge.spigot.event.PongReceiveEvent;
import dev.thomazz.pledge.spigot.event.TickEndEvent;
import dev.thomazz.pledge.spigot.event.TickStartEvent;
import dev.thomazz.pledge.spigot.reflection.ReflectionProvider;
import dev.thomazz.pledge.util.ChannelAccessProvider;
import dev.thomazz.pledge.util.ChannelUtils;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Getter
public class PledgeSpigot implements Pledge<Player>, Listener {

    /**
     * Creates a new API instance using the provided plugin to register listeners.
     * If an API instance already exists, it returns the existing one instead.
     * The API instance can be destroyed using {@link Pledge#destroy()}
     * <p>
     * @param plugin - Plugin to register listeners under
     * @return       - API instance
     */
    public static Pledge<Player> getOrCreate(@NotNull Plugin plugin) {
        if (PledgeSpigot.instance == null) {
            PledgeSpigot.instance = new PledgeSpigot(plugin);
        }

        return PledgeSpigot.instance;
    }

    static PledgeSpigot instance;

    private final Logger logger;
    private final PacketFiltering whitelist;
    private final ChannelAccessProvider channelAccessProvider;
    private final PingPacketProvider packetProvider;

    private final BukkitTask startTask;
    private final TickEndTask endTask;

    private final List<ClientPingerImpl<Player>> clientPingers = new ArrayList<>();
    private final Map<UUID, Channel> playerChannels = new HashMap<>();

    PledgeSpigot(Plugin plugin) {
        this.logger = plugin.getLogger();
        this.whitelist = new PacketFiltering(this);
        this.channelAccessProvider = new ChannelAccessProvider(this);
        this.packetProvider = PacketProviderFactory.buildPingProvider(this);

        PluginManager manager = plugin.getServer().getPluginManager();
        BukkitScheduler scheduler = plugin.getServer().getScheduler();

        TickEndTask.initialise(this);
        this.startTask = scheduler.runTaskTimer(plugin, () -> manager.callEvent(new TickStartEvent()), 0L, 1L);
        this.endTask = TickEndTask.create(() -> manager.callEvent(new TickEndEvent()));

        // Setup for all players
        Bukkit.getOnlinePlayers().forEach(this::setupPlayer);

        // Register as listener after setup
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void setupPlayer(Player player) {
        Channel channel;
        try {
            channel = channelAccessProvider.getChannel(player.getClass().getDeclaredMethod("getHandle").invoke(player), player.getUniqueId());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        this.playerChannels.put(player.getUniqueId(), channel);

        // Inject pong listener
        channel.pipeline().addBefore(
            "packet_handler",
            "pledge_packet_listener",
            new NetworkPongListener(this, player.getUniqueId())
        );

        // Register to client pingers
        this.clientPingers.forEach(pinger -> pinger.registerPlayer(player));
    }

    private void teardownPlayer(Player player) {
        this.playerChannels.remove(player.getUniqueId());

        // Unregister from client pingers
        this.clientPingers.forEach(pinger -> pinger.unregisterPlayer(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerLogin(PlayerLoginEvent event) {
        this.setupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
        this.teardownPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onTickStart(TickStartEvent ignored) {
        this.clientPingers.forEach(ClientPingerImpl::tickStart);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onTickStart(TickEndEvent ignored) {
        this.clientPingers.forEach(ClientPingerImpl::tickEnd);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPongReceive(PongReceiveEvent event) {
        UUID player = event.getPlayer();
        int id = event.getId();

        this.clientPingers.stream()
            .filter(pinger -> pinger.isInRange(id))
            .forEach(
                pinger -> pinger.getPingData(player)
                    .flatMap(data -> data.confirm(id))
                    .ifPresentOrElse(pong -> pinger.onReceive(player, pong), () -> pinger.onError(player, id))
            );
    }

    @Override
    public void sendPing(@NotNull UUID player, int id) {
        // Keep within ranges
        int max = Math.max(this.packetProvider.getUpperBound(), this.packetProvider.getLowerBound());
        int min = Math.min(this.packetProvider.getUpperBound(), this.packetProvider.getLowerBound());
        int pingId = Math.max(Math.min(id, max), min);

        // Run on channel event loop
        this.getChannel(player).ifPresent(channel ->
                ChannelUtils.runInEventLoop(channel, () ->
                        this.sendPingRaw(player, channel, pingId)
                )
        );
    }

    public void sendPingRaw(@NotNull UUID player, @NotNull Channel channel, int pingId) {
        try {
            Object packet = this.packetProvider.buildPacket(pingId);
            Bukkit.getPluginManager().callEvent(new PingSendEvent(player, pingId));
            channel.write(packet);
        } catch (Exception ex) {
            this.logger.severe(String.format("Failed to send ping! Player:%s Id:%o", player, pingId));
            ex.printStackTrace();
        }
    }

    @Override
    public Optional<Channel> getChannel(@NotNull UUID player) {
        return Optional.ofNullable(this.playerChannels.get(player));
    }

    @Override
    public ClientPinger<Player> createPinger(int startId, int endId) {
        ClientPingerImpl<Player> pinger = new ClientPingerImpl<>(this, startId, endId);
        this.clientPingers.add(pinger);
        return pinger;
    }

    @Override
    public FrameClientPinger<Player> createFramePinger(int startId, int endId) {
        FrameClientPingerImpl<Player> pinger = new FrameClientPingerImpl<>(this, startId, endId);
        this.clientPingers.add(pinger);
        return pinger;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public void destroy() {
        if (!this.equals(PledgeSpigot.instance)) {
            throw new IllegalStateException("API object not the same as current instance!");
        }

        // Teardown for all players
        Bukkit.getOnlinePlayers().forEach(this::teardownPlayer);

        HandlerList.unregisterAll(this);
        this.startTask.cancel();
        this.endTask.cancel();

        PledgeSpigot.instance = null;
    }

    private final EventProviderImpl eventProvider = new EventProviderImpl();

    @Override
    public EventProvider eventProvider() {
        return eventProvider;
    }

    private final ReflectionProvider reflectionProvider = new ReflectionProvider();

    @Override
    public MinecraftReflectionProvider getReflectionProvider() {
        return reflectionProvider;
    }

    @Override
    public PacketFiltering getPacketFilter() {
        return whitelist;
    }

    @Override
    public UUID asUUID(Player player) {
        return player.getUniqueId();
    }
}
