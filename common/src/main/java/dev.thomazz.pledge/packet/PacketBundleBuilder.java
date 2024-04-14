package dev.thomazz.pledge.packet;

import java.lang.reflect.Constructor;

public class PacketBundleBuilder {

    public static final PacketBundleBuilder INSTANCE = new PacketBundleBuilder();

    private final Class<?> bundleClass;
    private final Class<?> bundleClientboundClass;
    private final Constructor<?> bundleConstructor;

    public PacketBundleBuilder() {
        Class<?> bundleClass;
        Class<?> bundleClientboundClass;
        Constructor<?> constructor;

        try {
            bundleClass = Class.forName("net.minecraft.network.protocol.BundleDelimiterPacket");
            bundleClientboundClass = Class.forName("net.minecraft.network.protocol.game.ClientboundBundlePacket");
            constructor = bundleClass.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (Exception ex) {
            bundleClass = null;
            bundleClientboundClass = null;
            constructor = null;
        }

        this.bundleClass = bundleClass;
        this.bundleClientboundClass = bundleClientboundClass;
        this.bundleConstructor = constructor;
    }

    public Object buildDelimiter() throws Exception {
        return this.bundleConstructor.newInstance();
    }

    public boolean isDelimiter(Class<?> packetType) {
        return packetType.equals(bundleClass);
    }

    public boolean isClientboundBundle(Class<?> packetType) {
        return packetType.equals(bundleClientboundClass);
    }

    public boolean isSupported() {
        return this.bundleConstructor != null;
    }
}