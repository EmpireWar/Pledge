package dev.thomazz.pledge.pinger.frame.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Object containing ping packet IDs linked to the start and end of a tick.
 */
@RequiredArgsConstructor
@Getter
@Setter
public class Frame {
    private final int startId;
    private final int endId;
    private boolean bundle;
}
