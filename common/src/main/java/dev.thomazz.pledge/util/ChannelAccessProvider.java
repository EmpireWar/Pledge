package dev.thomazz.pledge.util;

import dev.thomazz.pledge.Pledge;
import io.netty.channel.Channel;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class ChannelAccessProvider {

    private final Pledge<?> pledge;

    private final Class<?> NETWORK_MANAGER_CLASS;

    private final Class<?> PACKET_LISTENER_CLASS;

    private final Class<?> PLAYER_CONNECTION_CLASS;

    public ChannelAccessProvider(Pledge<?> pledge) {
        this.pledge = pledge;
        final MinecraftReflectionProvider reflectionProvider = pledge.getReflectionProvider();
        this.NETWORK_MANAGER_CLASS = reflectionProvider.getMinecraftClass(
                "network.NetworkManager",
                "NetworkManager"
        );
        
        this.PACKET_LISTENER_CLASS = reflectionProvider.getMinecraftClass(
                "network.PacketListener",
                "PacketListener"
        );

        this.PLAYER_CONNECTION_CLASS = reflectionProvider.getMinecraftClass(
                "server.network.PlayerConnection",
                "PlayerConnection"
        );
    }

    public Channel getChannel(Object handle, UUID playerId) {
        try {
            Field playerConnectionField = ReflectionUtil.getFieldByType(handle.getClass(), PLAYER_CONNECTION_CLASS);
            Field networkManagerField = ReflectionUtil.getFieldByType(PLAYER_CONNECTION_CLASS, NETWORK_MANAGER_CLASS);
            Field channelField = ReflectionUtil.getFieldByType(NETWORK_MANAGER_CLASS, Channel.class);

            // Try the easy way first
            Object playerConnection = playerConnectionField.get(handle);
            if (playerConnection != null) {
                Object networkManager = networkManagerField.get(playerConnection);
                return (Channel) channelField.get(networkManager);
            }

            // Try to match all network managers after from game profile
            List<Object> networkManagers = getNetworkManagers();
            Field listenerField = ReflectionUtil.getFieldByType(NETWORK_MANAGER_CLASS, PACKET_LISTENER_CLASS);

            for (Object networkManager : networkManagers) {
                Object packetListener = listenerField.get(networkManager);
                if (packetListener != null) {
                    if (packetListener.getClass().getSimpleName().equals("LoginListener")) {
                        Field profileField = ReflectionUtil.getFieldByClassNames(packetListener.getClass(), "GameProfile");
                        Object gameProfile = profileField.get(packetListener);

                        // We can use the game profile to look up the player id in the listener
                        Field uuidField = ReflectionUtil.getFieldByType(gameProfile.getClass(), UUID.class);
                        UUID foundId = (UUID) uuidField.get(gameProfile);
                        if (playerId.equals(foundId)) {
                            return (Channel) channelField.get(networkManager);
                        }
                    } else {
                        // For player connection listeners we can get the player handle
                        Field playerField;
                        try {
                            playerField = ReflectionUtil.getFieldByClassNames(packetListener.getClass(), "EntityPlayer");
                        } catch (NoSuchFieldException ignored) {
                            // Might be ServerConfigurationPacketListenerImpl or something else that is unsupported
                            continue;
                        }

                        Object entityPlayer = playerField.get(packetListener);
                        if (handle.equals(entityPlayer)) {
                            return (Channel) channelField.get(networkManager);
                        }
                    }
                }
            }

            throw new NoSuchElementException("Did not find player channel!");
        } catch (Exception ex) {
            throw new RuntimeException("Could not get channel for player: " + playerId, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> getNetworkManagers() {
        try {
            Object serverConnection = pledge.getReflectionProvider().getServerConnection();
            for (Field field : serverConnection.getClass().getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType()) || !field.getGenericType().getTypeName().contains("NetworkManager")) {
                    continue;
                }

                field.setAccessible(true);

                List<Object> networkManagers = (List<Object>) field.get(serverConnection);
                return Collections.synchronizedList(networkManagers);
            }

            throw new NoSuchElementException("Did not find correct list in server connection");
        } catch (Exception ex) {
            throw new RuntimeException("Cannot retrieve network managers", ex);
        }
    }
}
