package dev.thomazz.pledge.pinger.frame;

import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.network.queue.ChannelMessageQueueHandler;
import dev.thomazz.pledge.network.queue.ChannelMessageQueuePrimer;
import dev.thomazz.pledge.network.queue.QueueMode;
import dev.thomazz.pledge.pinger.ClientPingerImpl;
import dev.thomazz.pledge.pinger.data.Ping;
import dev.thomazz.pledge.pinger.data.PingData;
import dev.thomazz.pledge.pinger.data.PingOrder;
import dev.thomazz.pledge.pinger.frame.data.Frame;
import dev.thomazz.pledge.pinger.frame.data.FrameData;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class FrameClientPingerImpl<SP> extends ClientPingerImpl<SP> implements FrameClientPinger<SP> {
    private final Map<UUID, FrameData> frameDataMap = new LinkedHashMap<>();
    private final List<FrameClientPingerListener> frameListener = new ArrayList<>();

    public FrameClientPingerImpl(Pledge<SP> clientPing, int startId, int endId) {
        super(clientPing, startId, endId);
    }

    @Override
    public void attach(FrameClientPingerListener listener) {
        super.attach(listener);
        this.frameListener.add(listener);
    }

    @Override
    public void registerPlayer(SP player) {
        super.registerPlayer(player);
        final UUID uuid = api.asUUID(player);
        this.injectPlayer(uuid);
        this.frameDataMap.put(uuid, new FrameData());
    }

    @Override
    public void unregisterPlayer(UUID player) {
        super.unregisterPlayer(player);
        this.ejectPlayer(player);
        this.frameDataMap.remove(player);
    }

    @Override
    public void tickStart() {
        this.frameDataMap.keySet().forEach(this::tryReadyHandler);
    }

    @Override
    public void tickEnd() {
        this.frameDataMap.forEach(this::trySendPings);
    }

    @Override
    protected void onReceiveStart(UUID player, int id) {
        super.onReceiveStart(player, id);

        FrameData data = this.frameDataMap.get(player);
        if (data != null && this.frameListener != null) {
            data.matchStart(id).ifPresent(
                frame -> this.frameListener.forEach(listener -> listener.onFrameReceiveStart(player, frame))
            );
        }
    }

    @Override
    protected void onReceiveEnd(UUID player, int id) {
        super.onReceiveEnd(player, id);

        FrameData data = this.frameDataMap.get(player);
        if (data != null && this.frameListener != null) {
            data.matchEnd(id).ifPresent(
                frame -> {
                    this.frameListener.forEach(listener -> listener.onFrameReceiveEnd(player, frame));
                    data.popFrame();
                }
            );
        }
    }

    @Override
    public synchronized Frame getOrCreate(UUID player) {
        PingData pingData = this.pingDataMap.get(player);
        FrameData frameData = this.frameDataMap.get(player);

        Objects.requireNonNull(pingData);
        Objects.requireNonNull(frameData);

        if (!frameData.hasFrame()) {
            frameData.setFrame(this.createFrame(player, pingData));
        }

        return frameData.getFrame();
    }

    public Optional<FrameData> getFrameData(SP player) {
        return Optional.ofNullable(this.frameDataMap.get(player));
    }

    private void tryReadyHandler(UUID player) {
        this.api.getChannel(player).filter(Channel::isOpen).ifPresent(channel ->
            channel.eventLoop().execute(() -> {
                try {
                    ChannelMessageQueueHandler handler = channel.pipeline().get(ChannelMessageQueueHandler.class);
                    handler.setMode(QueueMode.ADD_LAST);
                } catch (Exception ex) {
                    this.api.logger().severe("Unable to ready handler for player: " + player);
                    ex.printStackTrace();
                }
            })
        );
    }

    private void trySendPings(UUID player, FrameData frameData) {
        Optional<Frame> optionalFrame = frameData.continueFrame();

        this.api.getChannel(player).filter(Channel::isOpen).ifPresent(channel ->
            channel.eventLoop().execute(() -> {
                try {
                    ChannelMessageQueueHandler handler = channel.pipeline().get(ChannelMessageQueueHandler.class);

                    if (optionalFrame.isPresent()) {
                        Frame frame = optionalFrame.get();
                        this.frameListener.forEach(listener -> listener.onFrameSend(player, frame));

                        // Wrap by ping packets
                        handler.setMode(QueueMode.ADD_FIRST);
                        this.ping(player, new Ping(PingOrder.TICK_START, frame.getStartId()));
                        handler.setMode(QueueMode.ADD_LAST);
                        this.ping(player, new Ping(PingOrder.TICK_END, frame.getEndId()));
                    }

                    handler.drain(channel.pipeline().context(handler));
                } catch (Exception ex) {
                    this.api.logger().severe("Unable to drain message queue from player: " + player);
                    ex.printStackTrace();
                }
            })
        );
    }

    private Frame createFrame(UUID player, PingData data) {
        Frame frame = new Frame(data.pullId(), data.pullId());
        this.frameListener.forEach(listener -> listener.onFrameCreate(player, frame));
        return frame;
    }

    private void injectPlayer(UUID player) {
        ChannelMessageQueueHandler queueHandler = new ChannelMessageQueueHandler();
        ChannelMessageQueuePrimer queuePrimer = new ChannelMessageQueuePrimer(api, queueHandler);
        this.api.getChannel(player).ifPresent(
            channel -> channel.pipeline()
                .addFirst(
                    "pledge_queue_handler",
                    queueHandler
                )
                .addLast(
                    "pledge_queue_primer",
                    queuePrimer
                )
        );
    }

    private void ejectPlayer(UUID player) {
        this.api.getChannel(player).ifPresent(
            channel -> {
                channel.pipeline().remove(ChannelMessageQueueHandler.class);
                channel.pipeline().remove(ChannelMessageQueuePrimer.class);
            }
        );
    }
}
