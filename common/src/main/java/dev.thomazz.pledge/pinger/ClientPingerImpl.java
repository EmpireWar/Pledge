package dev.thomazz.pledge.pinger;

import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.network.NetworkPacketConsolidator;
import dev.thomazz.pledge.packet.PingPacketProvider;
import dev.thomazz.pledge.pinger.data.Ping;
import dev.thomazz.pledge.pinger.data.PingData;
import dev.thomazz.pledge.pinger.data.PingOrder;
import dev.thomazz.pledge.util.ChannelUtils;
import io.netty.channel.Channel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Getter
public class ClientPingerImpl<SP> implements ClientPinger<SP> {
    protected final Map<UUID, PingData> pingDataMap = new LinkedHashMap<>();
    protected final List<ClientPingerListener> pingListeners = new ArrayList<>();

    protected final Pledge<SP> api;
    protected final int startId;
    protected final int endId;

    protected Predicate<SP> playerFilter = player -> true;

    public ClientPingerImpl(Pledge<SP> api, int startId, int endId) {
        this.api = api;

        PingPacketProvider provider = api.getPacketProvider();
        int upperBound = provider.getUpperBound();
        int lowerBound = provider.getLowerBound();

        this.startId = Math.max(Math.min(upperBound, startId), lowerBound);
        this.endId = Math.max(Math.min(upperBound, endId), lowerBound);

        if (this.startId != startId) {
            this.api.logger().warning("Changed start ID to fit bounds: " + startId + " -> " + this.startId);
        }

        if (this.endId != endId) {
            this.api.logger().warning("Changed end ID to fit bounds: " + endId + " -> " + this.endId);
        }
    }

    @Override
    public int startId() {
        return this.startId;
    }

    @Override
    public int endId() {
        return this.endId;
    }

    @Override
    public void filter(Predicate<SP> condition) {
        this.playerFilter = condition;
    }

    @Override
    public void attach(ClientPingerListener listener) {
        this.pingListeners.add(listener);
    }

    public void registerPlayer(SP player) {
        if (this.playerFilter.test(player)) {
            final UUID uuid = api.asUUID(player);
            this.injectPlayer(uuid);
            this.pingDataMap.put(uuid, new PingData(uuid,this));
        }
    }

    public void unregisterPlayer(UUID player) {
        this.pingDataMap.remove(player);
        this.ejectPlayer(player);
    }

    protected void injectPlayer(UUID player) {
        this.api.getChannel(player).ifPresent(channel ->
                ChannelUtils.runInEventLoop(channel,
                        () -> channel.pipeline().addLast("pledge_tick_consolidator", new NetworkPacketConsolidator(api))
                )
        );
    }

    protected void ejectPlayer(UUID player) {
        this.api.getChannel(player).ifPresent(channel ->
                ChannelUtils.runInEventLoop(channel,
                        () -> channel.pipeline().remove(NetworkPacketConsolidator.class)
                )
        );
    }

    protected void ping(UUID player, Channel channel, Ping ping) {
        if (!channel.eventLoop().inEventLoop()) {
            throw new IllegalStateException("Tried to run ping outside event loop!");
        }

        this.api.sendPingRaw(player, channel, ping.getId());
        this.getPingData(player).ifPresent(data -> data.offer(ping));
        this.onSend(player, ping);
    }

    public boolean isInRange(int id) {
        return id >= Math.min(this.startId, this.endId) && id <= Math.max(this.startId, this.endId);
    }

    public Optional<PingData> getPingData(UUID player) {
        return Optional.of(this.pingDataMap.get(player));
    }

    protected void onSend(UUID player, Ping ping) {
        switch (ping.getOrder()) {
            case TICK_START:
                this.onSendStart(player, ping.getId());
                break;
            case TICK_END:
                this.onSendEnd(player, ping.getId());
                break;
        }
    }

    public void onReceive(UUID player, Ping ping) {
        switch (ping.getOrder()) {
            case TICK_START:
                this.onReceiveStart(player, ping.getId());
                break;
            case TICK_END:
                this.onReceiveEnd(player, ping.getId());
                break;
        }
    }

    public void onError(UUID player, int id) {
        this.pingListeners.forEach(listener -> listener.onError(player, id));
    }

    protected void onSendStart(UUID player, int id) {
        this.pingListeners.forEach(listener -> listener.onPingSendStart(player, id));
    }

    protected void onSendEnd(UUID player, int id) {
        this.pingListeners.forEach(listener -> listener.onPingSendEnd(player, id));
    }

    protected void onReceiveStart(UUID player, int id) {
        this.pingListeners.forEach(listener -> listener.onPongReceiveStart(player, id));
    }

    protected void onReceiveEnd(UUID player, int id) {
        this.pingListeners.forEach(listener -> listener.onPongReceiveEnd(player, id));
    }

    public void tickStart() {
        this.pingDataMap.forEach((player, data) ->
                this.api.getChannel(player).ifPresent(channel ->
                        ChannelUtils.runInEventLoop(channel, () -> {
                            NetworkPacketConsolidator consolidator = channel.pipeline().get(NetworkPacketConsolidator.class);
                            if (consolidator != null) {
                                consolidator.open();
                                this.ping(player, channel, new Ping(PingOrder.TICK_START, data.pullId()));
                                consolidator.drain(channel.pipeline().lastContext());
                            }
                        })
                )
        );
    }

    public void tickEnd() {
        this.pingDataMap.forEach((player, data) ->
                this.api.getChannel(player).ifPresent(channel ->
                        ChannelUtils.runInEventLoop(channel, () -> {
                            NetworkPacketConsolidator consolidator = channel.pipeline().get(NetworkPacketConsolidator.class);
                            if (consolidator != null) {
                                this.ping(player, channel, new Ping(PingOrder.TICK_END, data.pullId()));
                                consolidator.close();
                            }
                        })
                )
        );
    }
}
