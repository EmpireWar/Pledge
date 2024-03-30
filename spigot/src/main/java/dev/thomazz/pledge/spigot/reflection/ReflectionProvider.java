package dev.thomazz.pledge.spigot.reflection;

import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import org.bukkit.Bukkit;
import org.bukkit.Server;

public class ReflectionProvider implements MinecraftReflectionProvider {

    @Override
    public String getCraftBukkitPackage() {
        return Bukkit.getServer().getClass().getPackage().getName();
    }

    @Override
    public Object getNMSServer() throws ReflectiveOperationException {
        Server server = Bukkit.getServer();
        return server.getClass().getMethod("getServer").invoke(server);
    }
}
