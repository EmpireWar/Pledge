package dev.thomazz.pledge.util;

import java.lang.reflect.Field;
import java.util.Arrays;

public interface MinecraftReflectionProvider {

    default Class<?> gamePacket(String className) throws ClassNotFoundException {
        try {
            return Class.forName(getMinecraftPackageLegacy() + "." + className); // Legacy structure
        } catch (Exception ignored) {
        }

        try {
            return Class.forName("net.minecraft.network.protocol.game." + className); // Game packet
        } catch (Exception ignored) {
        }

        try {
            return Class.forName("net.minecraft.network.protocol.common." + className); // 1.20.2+ common packets
        } catch (Exception ignored) {
        }

        throw new ClassNotFoundException("Game packet class not found!");
    }

    default Class<?> getMinecraftClass(String... names) {
        String[] packageNames = new String[] {
                getMinecraftPackage(),
                getMinecraftPackageLegacy()
        };

        for (String packageName : packageNames) {
            for(String name : names) {
                try {
                    return Class.forName(packageName + "." + name);
                } catch (Throwable ignored) {
                }
            }
        }

        throw new RuntimeException("Could not find minecraft class: " + Arrays.toString(names));
    }

    String getCraftBukkitPackage();

    default String getMinecraftPackage() {
        return "net.minecraft";
    }

    default String getMinecraftPackageLegacy() {
        return getCraftBukkitPackage().replace("org.bukkit.craftbukkit", "net.minecraft.server");
    }

    default Object getServerConnection() throws Exception {
        Object minecraftServer = getNMSServer();
        Field connectionField = ReflectionUtil.getFieldByClassNames(minecraftServer.getClass().getSuperclass(), "ServerConnectionListener", "ServerConnection");
        return connectionField.get(minecraftServer);
    }

    Object getNMSServer() throws ReflectiveOperationException;
}
