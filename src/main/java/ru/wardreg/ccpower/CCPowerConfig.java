package ru.wardreg.ccpower;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/**
 * All energy costs of the mod, in RF (Forge Energy) per game tick.
 * The common config file is watched by Forge and reloaded on file change.
 */
public final class CCPowerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue IDLE = BUILDER
        .comment("RF per game tick consumed by a powered-on computer that is idle",
            "(no Lua work executed during the previous tick).",
            "Turned-off computers consume nothing. Set to 0 to disable the idle drain.")
        .defineInRange("power.idleRFPerTick", 2.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue ACTIVE = BUILDER
        .comment("Extra RF per game tick while a program is executing",
            "(the Lua machine performed work during the previous tick).")
        .defineInRange("power.activeRFPerTick", 10.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue PERIPHERAL_TICK = BUILDER
        .comment("Default RF per game tick for a peripheral type without an explicit entry",
            "in config/ccpower-peripherals.toml.")
        .defineInRange("power.peripheralRFPerTick", 1.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue PERIPHERAL_CALL = BUILDER
        .comment("Base RF surcharge for a peripheral method call. Per-type prices live in",
            "config/ccpower-peripherals.toml [rf_per_call]; freshly discovered types are",
            "seeded from this value at a curated multiple for well-known types.")
        .defineInRange("power.peripheralCallRF", 5.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue SURCHARGE_CAP = BUILDER
        .comment("Maximum total surcharge (peripheral calls) carried into a single tick.")
        .defineInRange("power.surchargeCapPerTick", 1000.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue COMPUTER_BUFFER = BUILDER
        .comment("Capacity of the stationary computer's internal RF buffer.")
        .defineInRange("power.computerBufferRF", 2_000L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue COMPUTER_PULL = BUILDER
        .comment("RF per game tick a stationary computer pulls from adjacent FE blocks.")
        .defineInRange("power.computerPullRFPerTick", 1_000L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue TURTLE_ACTION = BUILDER
        .comment("RF charged for every turtle action (move, dig, place, ...).")
        .defineInRange("turtle.actionRF", 4.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue TURTLE_BUFFER = BUILDER
        .comment("Capacity of the turtle internal RF battery. 0 disables the limit.")
        .defineInRange("turtle.bufferRF", 20_000L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue DOCK_BUFFER = BUILDER
        .comment("Internal buffer of the Recharge Dock block, in RF.")
        .defineInRange("dock.bufferRF", 50_000L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue DOCK_PULL = BUILDER
        .comment("RF per game tick the Recharge Dock pulls from adjacent blocks.")
        .defineInRange("dock.pullRFPerTick", 1000L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.LongValue DOCK_CHARGE = BUILDER
        .comment("RF per game tick the Recharge Dock transfers into a turtle's battery.")
        .defineInRange("dock.chargeRFPerTick", 250L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue LOGS = BUILDER
        .comment("Enable CC: Power log messages.")
        .define("logging.enabled", true);

    private static final ForgeConfigSpec.BooleanValue LOG_TRANSITIONS = BUILDER
        .comment("Log discharge shutdowns and denied power-ons of individual computers.")
        .define("logging.stateTransitions", true);

    private static final ForgeConfigSpec.BooleanValue LOG_DISCOVERY = BUILDER
        .comment("Log every peripheral type discovered and appended to ccpower-peripherals.toml.")
        .define("logging.peripheralDiscovery", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private CCPowerConfig() {}

    public static double idleRFPerTick() { return IDLE.get(); }
    public static double activeRFPerTick() { return ACTIVE.get(); }
    public static double peripheralRFPerTick() { return PERIPHERAL_TICK.get(); }
    public static double peripheralCallRF() { return PERIPHERAL_CALL.get(); }
    public static double surchargeCapPerTick() { return SURCHARGE_CAP.get(); }
    public static long computerBufferRF() { return COMPUTER_BUFFER.get(); }
    public static long computerPullRFPerTick() { return COMPUTER_PULL.get(); }
    public static double turtleActionRF() { return TURTLE_ACTION.get(); }
    public static long turtleBufferRF() { return TURTLE_BUFFER.get(); }
    public static long dockBufferRF() { return DOCK_BUFFER.get(); }
    public static long dockPullRFPerTick() { return DOCK_PULL.get(); }
    public static long dockChargeRFPerTick() { return DOCK_CHARGE.get(); }
    public static boolean loggingEnabled() { return LOGS.get(); }
    public static boolean logStateTransitions() { return LOG_TRANSITIONS.get(); }
    public static boolean logPeripheralDiscovery() { return LOG_DISCOVERY.get(); }

    public static void onConfigReloading(ModConfig.Reloading event) {
        if (event.getConfig().getSpec() == SPEC && loggingEnabled()) {
            CCPowerMod.LOG.info("CC: Power configuration reloaded");
        }
    }
}
