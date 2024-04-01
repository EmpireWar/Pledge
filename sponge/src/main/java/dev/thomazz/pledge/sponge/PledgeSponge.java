package dev.thomazz.pledge.sponge;

import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.event.EventProvider;
import dev.thomazz.pledge.network.NetworkPongListener;
import dev.thomazz.pledge.network.queue.PacketQueueWhitelist;
import dev.thomazz.pledge.packet.PacketProviderFactory;
import dev.thomazz.pledge.packet.PingPacketProvider;
import dev.thomazz.pledge.pinger.ClientPinger;
import dev.thomazz.pledge.pinger.ClientPingerImpl;
import dev.thomazz.pledge.pinger.frame.FrameClientPinger;
import dev.thomazz.pledge.pinger.frame.FrameClientPingerImpl;
import dev.thomazz.pledge.sponge.event.EventProviderImpl;
import dev.thomazz.pledge.sponge.event.PingSendEvent;
import dev.thomazz.pledge.sponge.event.PongReceiveEvent;
import dev.thomazz.pledge.sponge.event.TickEndEvent;
import dev.thomazz.pledge.sponge.event.TickStartEvent;
import dev.thomazz.pledge.sponge.reflection.ReflectionProvider;
import dev.thomazz.pledge.util.ChannelAccessProvider;
import dev.thomazz.pledge.util.ChannelUtils;
import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import dev.thomazz.pledge.util.TickEndTask;
import io.netty.channel.Channel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Getter
public final class PledgeSponge implements Pledge<User> {

    /**
     * Creates a new API instance using the provided plugin to register listeners.
     * If an API instance already exists, it returns the existing one instead.
     * The API instance can be destroyed using {@link Pledge#destroy()}
     * <p>
     * @param plugin - Plugin to register listeners under
     * @return       - API instance
     */
    public static Pledge<User> getOrCreate(@NotNull PluginContainer plugin) {
        if (PledgeSponge.instance == null) {
            PledgeSponge.instance = new PledgeSponge(plugin).start();
        }

        return PledgeSponge.instance;
    }

    static PledgeSponge instance;

    private final Logger logger;
    private final PacketQueueWhitelist whitelist;
    private final ChannelAccessProvider channelAccessProvider;
    private final PingPacketProvider packetProvider;

    private final ScheduledTask startTask;
    private final TickEndTask endTask;

    private final List<ClientPingerImpl<User>> clientPingers = new ArrayList<>();
    private final Map<UUID, Channel> playerChannels = new HashMap<>();

    PledgeSponge(PluginContainer plugin) {
        this.logger = Logger.getLogger(plugin.metadata().id());
        this.whitelist = new PacketQueueWhitelist(this);
        this.channelAccessProvider = new ChannelAccessProvider(this);
        this.packetProvider = PacketProviderFactory.buildPingProvider(this);

        final EventManager eventManager = Sponge.eventManager();
        final Scheduler scheduler = Sponge.server().scheduler();

        TickEndTask.initialise(this);
        this.startTask = scheduler.submit(Task.builder().plugin(plugin)
                .interval(Ticks.single()).execute(() -> eventManager.post(new TickStartEvent())).build(), "Pledge Tick Start");
        this.endTask = TickEndTask.create(() -> eventManager.post(new TickEndEvent()));

        Sponge.eventManager().registerListeners(plugin, this);
    }

    private void setupPlayer(User player, ServerSideConnection connection) {
        Channel channel;
        channel = channelAccessProvider.getChannel(connection, player.uniqueId());
        this.playerChannels.put(player.uniqueId(), channel);

        // Inject pong listener
        channel.pipeline().addBefore(
            "packet_handler",
            "pledge_packet_listener",
            new NetworkPongListener(this, player.uniqueId())
        );

        // Register to client pingers
        this.clientPingers.forEach(pinger -> pinger.registerPlayer(player));
    }

    private void teardownPlayer(ServerPlayer player) {
        this.playerChannels.remove(player.uniqueId());

        // Unregister from client pingers
        this.clientPingers.forEach(pinger -> pinger.unregisterPlayer(player.uniqueId()));
    }

    PledgeSponge start() {
        // Setup for all players
        Sponge.server().onlinePlayers().forEach(player -> this.setupPlayer(player.user(), player.connection()));
        return this;
    }

    @Listener(order = Order.LATE)
    void onPlayerLogin(ServerSideConnectionEvent.Login event) {
        this.setupPlayer(event.user(), event.connection());
    }

    @Listener(order = Order.LATE)
    void onPlayerQuit(ServerSideConnectionEvent.Disconnect event) {
        this.teardownPlayer(event.player());
    }

    @Listener(order = Order.LATE)
    void onTickStart(TickStartEvent ignored) {
        this.clientPingers.forEach(ClientPingerImpl::tickStart);
    }

    @Listener(order = Order.LATE)
    void onTickStart(TickEndEvent ignored) {
        this.clientPingers.forEach(ClientPingerImpl::tickEnd);
    }

    @Listener(order = Order.LATE)
    void onPongReceive(PongReceiveEvent event) {
        UUID player = event.getPlayer();
        int id = event.getId();

        this.clientPingers.stream()
            .filter(pinger -> pinger.isInRange(id))
            .forEach(
                pinger -> pinger.getPingData(player)
                    .flatMap(data -> data.confirm(id))
                    .ifPresent(pong -> pinger.onReceive(player, pong))
            );
    }

    @Override
    public void sendPing(@NotNull UUID player, int id) {
        try {
            final EventManager eventManager = Sponge.eventManager();
            Object packet = this.packetProvider.buildPacket(id);

            // Keep within ranges
            int max = Math.max(this.packetProvider.getUpperBound(), this.packetProvider.getLowerBound());
            int min = Math.min(this.packetProvider.getUpperBound(), this.packetProvider.getLowerBound());
            int pingId = Math.max(Math.min(id, max), min);

            // Run on channel event loop
            this.getChannel(player).ifPresent(channel ->
                    ChannelUtils.runInEventLoop(channel, () -> {
                        eventManager.post(new PingSendEvent(player, pingId));
                        channel.writeAndFlush(packet);
                    })
            );
        } catch (Exception ex) {
            this.logger.severe(String.format("Failed to send ping! Player:%s Id:%o", player, id));
            ex.printStackTrace();
        }
    }

    @Override
    public Optional<Channel> getChannel(@NotNull UUID player) {
        return Optional.ofNullable(this.playerChannels.get(player));
    }

    @Override
    public ClientPinger<User> createPinger(int startId, int endId) {
        ClientPingerImpl<User> pinger = new ClientPingerImpl<>(this, startId, endId);
        this.clientPingers.add(pinger);
        return pinger;
    }

    @Override
    public FrameClientPinger<User> createFramePinger(int startId, int endId) {
        FrameClientPingerImpl<User> pinger = new FrameClientPingerImpl<>(this, startId, endId);
        this.clientPingers.add(pinger);
        return pinger;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public void destroy() {
        if (!this.equals(PledgeSponge.instance)) {
            throw new IllegalStateException("API object not the same as current instance!");
        }

        // Teardown for all players
        Sponge.server().onlinePlayers().forEach(this::teardownPlayer);

        Sponge.eventManager().unregisterListeners(this);
        this.startTask.cancel();
        this.endTask.cancel();

        PledgeSponge.instance = null;
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
    public PacketQueueWhitelist getPacketQueueWhitelist() {
        return whitelist;
    }

    @Override
    public UUID asUUID(User player) {
        return player.uniqueId();
    }
}
