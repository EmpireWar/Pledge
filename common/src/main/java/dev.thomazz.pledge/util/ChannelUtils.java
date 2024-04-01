package dev.thomazz.pledge.util;

import io.netty.channel.Channel;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ChannelUtils {
    public void runInEventLoop(Channel channel, Runnable runnable) {
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().execute(runnable);
        } else {
            runnable.run();
        }
    }
}