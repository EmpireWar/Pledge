package dev.thomazz.pledge.util;

import dev.thomazz.pledge.api.Pledge;

import java.util.Arrays;

public interface MinecraftReflectionProvider {

    Class<?> legacyNms(String className) throws Exception;

    default Class<?> gamePacket(String className) throws Exception {
        // Support for legacy versions
        try {
            return legacyNms(className);
        } catch (Exception ignored) {
        }

        // Otherwise try the game packet class
        try {
            return Class.forName("net.minecraft.network.protocol.game." + className);
        } catch (Exception ignored) {}

        // Otherwise try common packet class for 1.20.2+
        return Class.forName("net.minecraft.network.protocol.common." + className);
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
        return getCraftBukkitPackage()
                .replace("org.bukkit.craftbukkit", "net.minecraft.server");
    }

    Object getServerConnection() throws Exception;

    Object getMinecraftServer() throws Exception;
}
