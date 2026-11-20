package ru.wardreg.ccpower;

import ru.wardreg.ccpower.platform.ConfigProvider;

/**
 * All energy costs of the mod, in RF (Forge Energy) per game tick.
 *
 * <p>The facade delegates to the loader's native config implementation:
 * ForgeConfigSpec on Forge, a tiny watched TOML file on Fabric. Values are
 * read on every access, so an on-disk reload is picked up immediately.
 */
public final class CCPowerConfig {
    private static ConfigProvider impl;

    public static void init(ConfigProvider provider) {
        impl = provider;
    }

    public static void tick() {
        if (impl != null) impl.tick();
    }

    public static double idleRFPerTick() { return impl.idleRFPerTick(); }
    public static double activeRFPerTick() { return impl.activeRFPerTick(); }
    public static double peripheralRFPerTick() { return impl.peripheralRFPerTick(); }
    public static double peripheralCallRF() { return impl.peripheralCallRF(); }
    public static double surchargeCapPerTick() { return impl.surchargeCapPerTick(); }
    public static long computerBufferRF() { return impl.computerBufferRF(); }
    public static long computerPullRFPerTick() { return impl.computerPullRFPerTick(); }
    public static double turtleActionRF() { return impl.turtleActionRF(); }
    public static long turtleBufferRF() { return impl.turtleBufferRF(); }
    public static long dockBufferRF() { return impl.dockBufferRF(); }
    public static long dockPullRFPerTick() { return impl.dockPullRFPerTick(); }
    public static long dockChargeRFPerTick() { return impl.dockChargeRFPerTick(); }
    public static boolean loggingEnabled() { return impl.loggingEnabled(); }
    public static boolean logStateTransitions() { return impl.logStateTransitions(); }
    public static boolean logPeripheralDiscovery() { return impl.logPeripheralDiscovery(); }

    private CCPowerConfig() {}
}
