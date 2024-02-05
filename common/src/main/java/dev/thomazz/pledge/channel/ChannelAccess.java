package dev.thomazz.pledge.channel;

import io.netty.channel.Channel;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;

public interface ChannelAccess {

    Channel getChannel(Object player, UUID playerId);

    Channel getChannel(InetAddress address);

    Collection<Channel> getAllChannels();
}
