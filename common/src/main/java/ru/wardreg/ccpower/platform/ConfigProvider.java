package ru.wardreg.ccpower.platform;

/**
 * Config facade implemented by the loader-specific config systems:
 * ForgeConfigSpec on Forge, a tiny self-managed TOML reader on Fabric.
 * Values are read on every access so on-disk reloads are picked up at once.
 */
public interface ConfigProvider {
    double idleRFPerTick();
    double activeRFPerTick();
    double peripheralRFPerTick();
    double peripheralCallRF();
    double surchargeCapPerTick();
    long computerBufferRF();
    long computerPullRFPerTick();
    double turtleActionRF();
    long turtleBufferRF();
    long dockBufferRF();
    long dockPullRFPerTick();
    long dockChargeRFPerTick();
    boolean loggingEnabled();
    boolean logStateTransitions();
    boolean logPeripheralDiscovery();

    /** Poll for an on-disk change (no-op for systems with a native watcher). */
    default void tick() {}
}
