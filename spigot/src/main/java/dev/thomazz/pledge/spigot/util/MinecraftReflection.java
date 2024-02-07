package dev.thomazz.pledge.spigot.util;

import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import dev.thomazz.pledge.util.ReflectionUtil;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;

public class MinecraftReflection implements MinecraftReflectionProvider {

    private static final String BASE = Bukkit.getServer().getClass().getPackage().getName();
    private static final String NMS = MinecraftReflection.BASE.replace("org.bukkit.craftbukkit", "net.minecraft.server");

    @Override
    public Class<?> legacyNms(String className) throws Exception {
        return Class.forName(MinecraftReflection.NMS + "." + className);
    }

    @Override
    public String getCraftBukkitPackage() {
        return Bukkit.getServer().getClass().getPackage().getName();
    }

    @Override
    public Object getServerConnection() throws Exception {
        Object minecraftServer = getMinecraftServer();
        Field connectionField = ReflectionUtil.getFieldByClassNames(minecraftServer.getClass().getSuperclass(), "ServerConnectionListener", "ServerConnection");
        return connectionField.get(minecraftServer);
    }

    @Override
    public Object getMinecraftServer() throws Exception {
        return Bukkit.getServer().getClass().getDeclaredMethod("getServer").invoke(Bukkit.getServer());
    }
}
