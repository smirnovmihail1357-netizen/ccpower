package ru.wardreg.ccpower.debug;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player flag for the optional diagnostic readout. Toggled by the
 * {@code /ccpower debug} command; available to integrations that want to show
 * extra power details to a specific player.
 */
public final class DebugState {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private DebugState() {}

    /** Toggle the flag for a player and return its new value. */
    public static boolean toggle(UUID player) {
        if (ENABLED.remove(player)) return false;
        ENABLED.add(player);
        return true;
    }

    public static boolean isEnabled(UUID player) {
        return player != null && ENABLED.contains(player);
    }
}
