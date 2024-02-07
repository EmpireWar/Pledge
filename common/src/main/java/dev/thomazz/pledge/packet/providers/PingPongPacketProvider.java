package dev.thomazz.pledge.packet.providers;

import dev.thomazz.pledge.api.Pledge;
import dev.thomazz.pledge.packet.PacketProvider;
import dev.thomazz.pledge.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class PingPongPacketProvider implements PacketProvider {
    private final Class<?> pingClass;
    private final Class<?> pongClass;
    private final Class<?> loginClass;
    private final Class<?> keepAliveClass;
    private final Class<?> disconnectClass;

    private final Field pongIdField;
    private final Constructor<?> pingConstructor;

    public PingPongPacketProvider(Pledge<?, ?> pledge) throws Exception {
        this.pingClass = pledge.reflectionProvider().gamePacket("ClientboundPingPacket");
        this.pongClass = pledge.reflectionProvider().gamePacket("ServerboundPongPacket");
        this.loginClass = getLoginClass(pledge);
        this.keepAliveClass = getKeepAliveClass(pledge);
        this.disconnectClass = getDisconnectClass(pledge);

        this.pongIdField = ReflectionUtil.getFieldByType(this.pongClass, int.class);
        this.pingConstructor = this.pingClass.getConstructor(int.class);
    }

    private Class<?> getLoginClass(Pledge<?, ?> pledge) throws Exception {
        try {
            return pledge.reflectionProvider().gamePacket("PacketPlayOutLogin");
        } catch (Exception ignored) {
            // 1.20.2+
            return pledge.reflectionProvider().gamePacket("ClientboundLoginPacket");
        }
    }

    private Class<?> getKeepAliveClass(Pledge<?, ?> pledge) throws Exception {
        try {
            return pledge.reflectionProvider().gamePacket("PacketPlayOutKeepAlive");
        } catch (Exception ignored) {
            // 1.20.2+
            return pledge.reflectionProvider().gamePacket("ClientboundKeepAlivePacket");
        }
    }

    private Class<?> getDisconnectClass(Pledge<?, ?> pledge) throws Exception {
        try {
            return pledge.reflectionProvider().gamePacket("PacketPlayOutKickDisconnect");
        } catch (Exception ignored) {
            // 1.20.2+
            return pledge.reflectionProvider().gamePacket("ClientboundDisconnectPacket");
        }
    }

    @Override
    public Object buildPacket(int id) throws Exception {
        return this.pingConstructor.newInstance(id);
    }

    @Override
    public Integer idFromPacket(Object packet) throws Exception {
        if (this.pongClass.isInstance(packet)) {
            return this.pongIdField.getInt(packet);
        }

        return null;
    }

    @Override
    public boolean isLogin(Object packet) {
        return this.loginClass.isInstance(packet);
    }

    @Override
    public boolean isKeepAlive(Object packet) {
        return this.keepAliveClass.isInstance(packet);
    }

    @Override
    public boolean isDisconnect(Object packet) {
        return this.disconnectClass.isInstance(packet);
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
