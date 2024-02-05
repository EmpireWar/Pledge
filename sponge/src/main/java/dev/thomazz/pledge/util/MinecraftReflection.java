package dev.thomazz.pledge.util;

import org.spongepowered.api.Sponge;

import java.lang.reflect.Field;

public class MinecraftReflection implements MinecraftReflectionProvider {

    private static final String NMS = "net.minecraft.server";

    @Override
    public Class<?> legacyNms(String className) throws Exception {
        return Class.forName(MinecraftReflection.NMS + "." + className);
    }

    @Override
    public String getCraftBukkitPackage() {
        return "org.bukkit.craftbukkit";
    }

    @Override
    public Object getServerConnection() throws Exception {
        Object minecraftServer = getMinecraftServer();
        Field connectionField = ReflectionUtil.getFieldByClassNames(minecraftServer.getClass().getSuperclass(), "ServerConnection");
        return connectionField.get(minecraftServer);
    }

    @Override
    public Object getMinecraftServer() {
        return Sponge.server();
    }
}
