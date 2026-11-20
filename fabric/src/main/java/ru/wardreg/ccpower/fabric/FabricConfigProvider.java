package ru.wardreg.ccpower.fabric;

import ru.wardreg.ccpower.platform.ConfigProvider;
import ru.wardreg.ccpower.platform.TomlStore;

/** Fabric config: a small self-managed, watched TOML file. */
public final class FabricConfigProvider implements ConfigProvider {
    private final TomlStore store;

    public FabricConfigProvider(java.nio.file.Path configDir) {
        store = new TomlStore(configDir.resolve("ccpower-common.toml"));
        store.define("power.idleRFPerTick", 2.0, "RF per game tick while a computer is idle");
        store.define("power.activeRFPerTick", 10.0, "Extra RF per game tick while a program runs");
        store.define("power.peripheralRFPerTick", 1.0, "Default RF/tick for a peripheral type");
        store.define("power.peripheralCallRF", 5.0, "Base RF surcharge for a peripheral call");
        store.define("power.surchargeCapPerTick", 1000.0, "Max carried-over surcharge per tick");
        store.define("power.computerBufferRF", 2000L, "Internal RF buffer of a computer");
        store.define("power.computerPullRFPerTick", 1000L, "RF/tick pulled from adjacent FE blocks");
        store.define("turtle.actionRF", 4.0, "RF charged per turtle action");
        store.define("turtle.bufferRF", 20000L, "Capacity of the turtle RF battery");
        store.define("dock.bufferRF", 50000L, "Internal buffer of the Recharge Dock");
        store.define("dock.pullRFPerTick", 1000L, "RF/tick the dock pulls from neighbours");
        store.define("dock.chargeRFPerTick", 250L, "RF/tick the dock transfers to a turtle");
        store.define("logging.enabled", true, "Enable CC: Power log messages");
        store.define("logging.stateTransitions", true, "Log shutdowns and denied power-ons");
        store.define("logging.peripheralDiscovery", true, "Log newly discovered peripheral types");
        store.load();
    }

    public void tick() { store.tick(); }

    public double idleRFPerTick() { return store.getDouble("power.idleRFPerTick"); }
    public double activeRFPerTick() { return store.getDouble("power.activeRFPerTick"); }
    public double peripheralRFPerTick() { return store.getDouble("power.peripheralRFPerTick"); }
    public double peripheralCallRF() { return store.getDouble("power.peripheralCallRF"); }
    public double surchargeCapPerTick() { return store.getDouble("power.surchargeCapPerTick"); }
    public long computerBufferRF() { return store.getLong("power.computerBufferRF"); }
    public long computerPullRFPerTick() { return store.getLong("power.computerPullRFPerTick"); }
    public double turtleActionRF() { return store.getDouble("turtle.actionRF"); }
    public long turtleBufferRF() { return store.getLong("turtle.bufferRF"); }
    public long dockBufferRF() { return store.getLong("dock.bufferRF"); }
    public long dockPullRFPerTick() { return store.getLong("dock.pullRFPerTick"); }
    public long dockChargeRFPerTick() { return store.getLong("dock.chargeRFPerTick"); }
    public boolean loggingEnabled() { return store.getBoolean("logging.enabled"); }
    public boolean logStateTransitions() { return store.getBoolean("logging.stateTransitions"); }
    public boolean logPeripheralDiscovery() { return store.getBoolean("logging.peripheralDiscovery"); }
}
