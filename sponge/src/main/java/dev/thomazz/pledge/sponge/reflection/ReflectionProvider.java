package dev.thomazz.pledge.sponge.reflection;

import dev.thomazz.pledge.util.MinecraftReflectionProvider;
import org.spongepowered.api.Sponge;

public class ReflectionProvider implements MinecraftReflectionProvider {

    @Override
    public String getCraftBukkitPackage() {
        return "org.bukkit.craftbukkit";
    }

    @Override
    public Object getNMSServer() {
        return Sponge.server();
    }
}
