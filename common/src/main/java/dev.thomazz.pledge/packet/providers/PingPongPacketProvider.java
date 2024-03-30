package dev.thomazz.pledge.packet.providers;

import dev.thomazz.pledge.Pledge;
import dev.thomazz.pledge.packet.PingPacketProvider;
import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import dev.thomazz.pledge.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class PingPongPacketProvider implements PingPacketProvider {
    private final Class<?> pongClass;
    private final Field pongIdField;
    private final Constructor<?> pingConstructor;

    public PingPongPacketProvider(Pledge<?> pledge) throws Exception {
        final MinecraftReflectionProvider reflectionProvider = pledge.getReflectionProvider();
        this.pongClass = reflectionProvider.gamePacket("ServerboundPongPacket");
        this.pongIdField = ReflectionUtil.getFieldByType(this.pongClass, int.class);

        Class<?> pingClass = reflectionProvider.gamePacket("ClientboundPingPacket");
        this.pingConstructor = pingClass.getConstructor(int.class);
    }

    @Override
    public Object buildPacket(int id) throws Exception {
        return this.pingConstructor.newInstance(id);
    }

    @Override
    public int idFromPong(Object packet) throws Exception {
        return this.pongIdField.getInt(packet);
    }

    @Override
    public boolean isPong(Object packet) {
        return this.pongClass.isInstance(packet);
    }

    @Override
    public int getLowerBound() {
        return Integer.MIN_VALUE;
    }

    @Override
    public int getUpperBound() {
        return Integer.MAX_VALUE;
    }
}
