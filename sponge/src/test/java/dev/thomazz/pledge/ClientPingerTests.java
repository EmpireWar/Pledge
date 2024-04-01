package dev.thomazz.pledge;

import dev.thomazz.pledge.packet.PingPacketProvider;
import dev.thomazz.pledge.pinger.ClientPingerImpl;
import dev.thomazz.pledge.pinger.ClientPingerListener;
import dev.thomazz.pledge.pinger.data.PingData;
import dev.thomazz.pledge.pinger.frame.FrameClientPingerImpl;
import dev.thomazz.pledge.pinger.frame.FrameClientPingerListener;
import dev.thomazz.pledge.pinger.frame.data.Frame;
import dev.thomazz.pledge.pinger.frame.data.FrameData;
import dev.thomazz.pledge.sponge.PledgeSponge;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spongepowered.api.entity.living.player.User;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Sponge Client Pinger Tests")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClientPingerTests {
    @Mock private PledgeSponge clientPing;
    @Mock private PingPacketProvider provider;
    @Mock private User player;

    private EmbeddedChannel channel;

    @BeforeEach
    public void setupMocks() {
        when(this.clientPing.getPacketProvider()).thenReturn(this.provider);
        when(this.clientPing.getChannel(this.player.uniqueId())).thenReturn(Optional.of(this.channel = new EmbeddedChannel()));

        when(this.provider.getUpperBound()).thenReturn(0);
        when(this.provider.getLowerBound()).thenReturn(-999);
    }

    @Test
    @Order(1)
    public void testSimpleClientPinger() {
        ClientPingerImpl<User> pinger = new ClientPingerImpl<>(this.clientPing, 0, -999);
        pinger.registerPlayer(this.player);
        PingData pingData = pinger.getPingData(this.player.uniqueId()).orElseThrow(IllegalStateException::new);

        for (int i = 0; i < 200; i++) {
            pinger.tickStart();
            pinger.tickEnd();
            this.channel.runPendingTasks();
        }

        // Assert 400 ping being sent
        assertEquals(-400, pingData.getId());

        for (int i = 0; i > -400; i--) {
            if (!pingData.confirm(i).isPresent()) {
                fail("Not found: " + i);
            }
        }

        for (int i = 0; i < 400; i++) {
            pinger.tickStart();
            pinger.tickEnd();
            this.channel.runPendingTasks();
        }

        // Assert range overflow
        assertEquals(-200, pingData.getId());
    }

    @Test
    @Order(2)
    public void testFrameClientPinger() {
        FrameClientPingerImpl<User> pinger = new FrameClientPingerImpl<>(this.clientPing, 0, -999);
        pinger.registerPlayer(this.player);

        PingData pingData = pinger.getPingData(this.player.uniqueId()).orElseThrow(IllegalStateException::new);
        FrameData frameData = pinger.getFrameData(this.player.uniqueId()).orElseThrow(IllegalStateException::new);

        for (int i = 0; i < 200; i++) {
            pinger.tickStart();
            pinger.getOrCreate(this.player.uniqueId());
            pinger.tickEnd();
            this.channel.runPendingTasks();
        }

        // Assert 400 ping being sent
        assertEquals(-400, pingData.getId());

        boolean toggle = true;
        for (int i = 0; i > -400; i--) {
            Optional<Frame> frame;
            if (toggle) {
                frame = frameData.matchStart(i);
            } else {
                frame = frameData.matchEnd(i);
            }

            if (!frame.isPresent()) {
                fail("Not found: " + i);
            }

            if (!toggle) {
                frameData.popFrame();
            }

            toggle = !toggle;
        }

        for (int i = 0; i < 400; i++) {
            pinger.tickStart();
            pinger.getOrCreate(this.player.uniqueId());
            pinger.tickEnd();
            this.channel.runPendingTasks();
        }

        // Assert range overflow
        assertEquals(-200, pingData.getId());
    }

    @Test
    @Order(3)
    public void testClientPingerListener() {
        final UUID uuid = player.uniqueId();
        ClientPingerImpl<User> pinger = new ClientPingerImpl<>(this.clientPing, 0, -999);
        pinger.registerPlayer(this.player);

        PingData pingData = pinger.getPingData(uuid).orElseThrow(IllegalStateException::new);

        ClientPingerListener listener = mock(ClientPingerListener.class);
        pinger.attach(listener);

        for (int i = 0; i < 2; i++) {
            pinger.tickStart();
            pinger.tickEnd();
            this.channel.runPendingTasks();
        }

        pinger.onReceive(uuid, pingData.confirm(0).orElseThrow(IllegalStateException::new));
        pinger.onReceive(uuid, pingData.confirm(-1).orElseThrow(IllegalStateException::new));
        pinger.onReceive(uuid, pingData.confirm(-2).orElseThrow(IllegalStateException::new));
        pinger.onReceive(uuid, pingData.confirm(-3).orElseThrow(IllegalStateException::new));

        verify(listener, times(1)).onValidation(eq(uuid), anyInt());
        verify(listener, times(2)).onPingSendStart(eq(uuid), anyInt());
        verify(listener, times(2)).onPingSendEnd(eq(uuid), anyInt());
        verify(listener, times(2)).onPongReceiveStart(eq(uuid), anyInt());
        verify(listener, times(2)).onPongReceiveEnd(eq(uuid), anyInt());
    }

    @Test
    @Order(4)
    public void testFrameClientPingerListener() {
        final UUID uuid = player.uniqueId();
        FrameClientPingerImpl<User> pinger = new FrameClientPingerImpl<>(this.clientPing, 0, -999);
        pinger.registerPlayer(this.player);

        PingData pingData = pinger.getPingData(uuid).orElseThrow(IllegalStateException::new);

        FrameClientPingerListener listener = mock(FrameClientPingerListener.class);
        pinger.attach(listener);

        for (int i = 0; i < 2; i++) {
            pinger.tickStart();
            pinger.getOrCreate(uuid);
            pinger.tickEnd();
            this.channel.runPendingTasks();
        }

        pinger.onReceive(uuid, pingData.confirm(0).orElseThrow(IllegalStateException::new));
        pinger.onReceive(uuid, pingData.confirm(-1).orElseThrow(IllegalStateException::new));
        pinger.onReceive(uuid, pingData.confirm(-2).orElseThrow(IllegalStateException::new));
        pinger.onReceive(uuid, pingData.confirm(-3).orElseThrow(IllegalStateException::new));

        verify(listener, times(1)).onValidation(eq(uuid), anyInt());
        verify(listener, times(2)).onPingSendStart(eq(uuid), anyInt());
        verify(listener, times(2)).onPingSendEnd(eq(uuid), anyInt());
        verify(listener, times(2)).onPongReceiveStart(eq(uuid), anyInt());
        verify(listener, times(2)).onPongReceiveEnd(eq(uuid), anyInt());

        verify(listener, times(2)).onFrameCreate(eq(uuid), any());
        verify(listener, times(2)).onFrameSend(eq(uuid), any());
        verify(listener, times(2)).onFrameReceiveStart(eq(uuid), any());
        verify(listener, times(2)).onFrameReceiveEnd(eq(uuid), any());
    }
}