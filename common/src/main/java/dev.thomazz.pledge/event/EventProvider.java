package dev.thomazz.pledge.event;

import java.util.UUID;

public interface EventProvider {

    boolean callPongReceive(UUID player, int id);
}
