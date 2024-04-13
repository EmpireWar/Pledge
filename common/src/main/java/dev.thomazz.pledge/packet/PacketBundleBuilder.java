package dev.thomazz.pledge.packet;

import java.lang.reflect.Constructor;

public class PacketBundleBuilder {

    public static final PacketBundleBuilder INSTANCE = new PacketBundleBuilder();

    private final Class<?> bundleClass;
    private final Constructor<?> bundleConstructor;

    public PacketBundleBuilder() {
        Class<?> bundleClass;
        Constructor<?> constructor;

        try {
            bundleClass = Class.forName("net.minecraft.network.protocol.BundleDelimiterPacket");
            constructor = bundleClass.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (Exception ex) {
            bundleClass = null;
            constructor = null;
        }

        this.bundleClass = bundleClass;
        this.bundleConstructor = constructor;
    }

    public Object buildDelimiter() throws Exception {
        return this.bundleConstructor.newInstance();
    }

    public boolean isDelimiter(Class<?> packetType) {
        return packetType.equals(bundleClass);
    }

    public boolean isSupported() {
        return this.bundleConstructor != null;
    }
}