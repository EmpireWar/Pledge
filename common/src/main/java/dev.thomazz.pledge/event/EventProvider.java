package dev.thomazz.pledge.event;

import java.util.UUID;

public interface EventProvider {

    void callPongReceive(UUID player, int id);
}
