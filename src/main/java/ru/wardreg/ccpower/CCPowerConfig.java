package ru.wardreg.ccpower;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import ru.wardreg.ccpower.power.CostMode;

/**
 * All energy costs of the mod, in RF (Forge Energy) per game tick.
 *
 * <p>The common config file ({@code ccpower-common.toml}) is watched by NeoForge and
 * reloaded dynamically whenever it changes on disk - no restart is required.
 */
public final class CCPowerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue IDLE = BUILDER
        .comment("RF per game tick consumed by a powered-on computer that is idle",
            "(no Lua work executed during the previous tick).",
            "Turned-off computers consume nothing. Set to 0 to disable the idle drain.")
        .defineInRange("power.idleRFPerTick", 2.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue ACTIVE = BUILDER
        .comment("Extra RF per game tick while a program is executing",
            "(the Lua machine performed work during the previous tick).")
        .defineInRange("power.activeRFPerTick", 10.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue PERIPHERAL_TICK = BUILDER
        .comment("Default RF per game tick for a peripheral type without an explicit entry",
            "in config/ccpower-peripherals.toml. That file is auto-populated with",
            "every peripheral type found in the world, so this value only applies",
            "to types the moment they are first discovered (and to unknown types).")
        .defineInRange("power.peripheralRFPerTick", 1.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue PERIPHERAL_CALL = BUILDER
        .comment("Base RF surcharge for a peripheral method call. Every type's",
            "actual price lives in the [rf_per_call] section of",
            "config/ccpower-peripherals.toml: freshly discovered types are",
            "seeded from this value - well-known types at a curated multiple",
            "(item handlers like chests cost more, monitors and modems less),",
            "unknown types at the plain base value. The surcharge is added to",
            "the cost of the next game tick.")
        .defineInRange("power.peripheralCallRF", 5.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue SURCHARGE_CAP = BUILDER
        .comment("Maximum total surcharge (peripheral calls) carried over into a",
            "single tick, in RF. Prevents a spamming program from creating an",
            "unpayable energy bill.")
        .defineInRange("power.surchargeCapPerTick", 1000.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.LongValue COMPUTER_BUFFER = BUILDER
        .comment("Capacity of the stationary computer's internal RF buffer.",
            "The computer draws its per-tick cost from this buffer and refills",
            "it from adjacent FE blocks. A larger buffer rides through longer",
            "supply interruptions without shutting the computer down.")
        .defineInRange("power.computerBufferRF", 2_000L, 0L, Long.MAX_VALUE);

    private static final ModConfigSpec.LongValue COMPUTER_PULL = BUILDER
        .comment("RF per game tick a stationary computer pulls from adjacent FE",
            "blocks into its internal buffer.")
        .defineInRange("power.computerPullRFPerTick", 1_000L, 0L, Long.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue TURTLE_ACTION = BUILDER
        .comment("RF charged for every turtle action (move, dig, place, ...).",
            "Intended to be cheaper than a peripheral call.")
        .defineInRange("turtle.actionRF", 4.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.LongValue TURTLE_BUFFER = BUILDER
        .comment("Capacity of the turtle internal RF battery.",
            "Turtles carry a small buffer so they can drive between Recharge Docks;",
            "it is filled only at a Recharge Dock. 0 disables the battery limit.")
        .defineInRange("turtle.bufferRF", 20_000L, 0L, Long.MAX_VALUE);

    private static final ModConfigSpec.LongValue DOCK_BUFFER = BUILDER
        .comment("Internal buffer of the Recharge Dock block, in RF.")
        .defineInRange("dock.bufferRF", 50_000L, 0L, Long.MAX_VALUE);

    private static final ModConfigSpec.LongValue DOCK_PULL = BUILDER
        .comment("RF per game tick the Recharge Dock pulls from adjacent blocks.")
        .defineInRange("dock.pullRFPerTick", 1000L, 0L, Long.MAX_VALUE);

    private static final ModConfigSpec.LongValue DOCK_CHARGE = BUILDER
        .comment("RF per game tick the Recharge Dock transfers into the battery of",
            "the turtle standing on top of it.")
        .defineInRange("dock.chargeRFPerTick", 250L, 0L, Long.MAX_VALUE);

    // --- Portable devices: pocket computers and Smart Glasses ---------------
    // Each device has its own cost mode, multiplier/fixed value, buffer and
    // peripheral prices. MULTIPLIER and FIXED are exclusive via one enum.

    private static final ModConfigSpec.EnumValue<CostMode> POCKET_MODE = BUILDER
        .comment("Pocket computers: MULTIPLIER derives the cost from a normal computer,",
            "FIXED uses pocket.fixedRFPerTick instead. Mutually exclusive.")
        .defineEnum("power.pocket.mode", CostMode.MULTIPLIER);

    private static final ModConfigSpec.DoubleValue POCKET_MULTIPLIER = BUILDER
        .comment("Pocket computer cost multiplier (1.0 = same as a normal computer).")
        .defineInRange("power.pocket.multiplier", 1.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue POCKET_FIXED = BUILDER
        .comment("Pocket computer RF/tick when mode = FIXED.")
        .defineInRange("power.pocket.fixedRFPerTick", 2.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.LongValue POCKET_BUFFER = BUILDER
        .comment("Internal RF buffer of a pocket computer. It starts full and is not refilled.")
        .defineInRange("power.pocket.bufferRF", 2_000L, 0L, Long.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue POCKET_PERIPHERAL_TICK = BUILDER
        .comment("Pocket computer: RF/tick per attached peripheral when not using computer prices.")
        .defineInRange("power.pocket.peripheralRFPerTick", 1.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue POCKET_PERIPHERAL_CALL = BUILDER
        .comment("Pocket computer: RF per peripheral method call when not using computer prices.")
        .defineInRange("power.pocket.peripheralCallRF", 5.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue POCKET_USE_COMPUTER_PERIPHERALS = BUILDER
        .comment("true: use the peripheral prices configured for normal computers",
            "(ccpower-peripherals.toml). false: use power.pocket.peripheral* below.")
        .define("power.pocket.useComputerPeripheralCosts", true);

    private static final ModConfigSpec.EnumValue<CostMode> GLASSES_MODE = BUILDER
        .comment("Smart Glasses: MULTIPLIER derives the cost from a normal computer,",
            "FIXED uses smartGlasses.fixedRFPerTick instead. Mutually exclusive.")
        .defineEnum("power.smartGlasses.mode", CostMode.MULTIPLIER);

    private static final ModConfigSpec.DoubleValue GLASSES_MULTIPLIER = BUILDER
        .comment("Smart Glasses cost multiplier (1.0 = same as a normal computer).")
        .defineInRange("power.smartGlasses.multiplier", 0.75, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue GLASSES_FIXED = BUILDER
        .comment("Smart Glasses RF/tick when mode = FIXED.")
        .defineInRange("power.smartGlasses.fixedRFPerTick", 2.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.LongValue GLASSES_BUFFER = BUILDER
        .comment("Internal RF buffer of Smart Glasses. Starts full, not refilled.")
        .defineInRange("power.smartGlasses.bufferRF", 2_000L, 0L, Long.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue GLASSES_PERIPHERAL_TICK = BUILDER
        .comment("Smart Glasses: RF/tick per attached peripheral when not using computer prices.")
        .defineInRange("power.smartGlasses.peripheralRFPerTick", 1.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue GLASSES_PERIPHERAL_CALL = BUILDER
        .comment("Smart Glasses: RF per peripheral call when not using computer prices.")
        .defineInRange("power.smartGlasses.peripheralCallRF", 5.0, 0.0, Double.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue GLASSES_USE_COMPUTER_PERIPHERALS = BUILDER
        .comment("true: use the peripheral prices configured for normal computers",
            "(ccpower-peripherals.toml). false: use power.smartGlasses.peripheral* below.")
        .define("power.smartGlasses.useComputerPeripheralCosts", true);

    private static final ModConfigSpec.BooleanValue LOGS = BUILDER
        .comment("Enable CC: Power log messages.")
        .define("logging.enabled", true);

    private static final ModConfigSpec.BooleanValue LOG_TRANSITIONS = BUILDER
        .comment("Log discharge shutdowns and denied power-ons of individual computers.")
        .define("logging.stateTransitions", true);

    private static final ModConfigSpec.BooleanValue LOG_DISCOVERY = BUILDER
        .comment("Log every peripheral type the mod discovers in the world and appends",
            "to ccpower-peripherals.toml.")
        .define("logging.peripheralDiscovery", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private CCPowerConfig() {}

    // The config values are read through the spec on every access, so an on-disk
    // reload is picked up immediately without any caching layer.

    public static double idleRFPerTick() {
        return IDLE.get();
    }

    public static double activeRFPerTick() {
        return ACTIVE.get();
    }

    public static double peripheralRFPerTick() {
        return PERIPHERAL_TICK.get();
    }

    public static double peripheralCallRF() {
        return PERIPHERAL_CALL.get();
    }

    public static double surchargeCapPerTick() {
        return SURCHARGE_CAP.get();
    }

    public static long computerBufferRF() {
        return COMPUTER_BUFFER.get();
    }

    public static long computerPullRFPerTick() {
        return COMPUTER_PULL.get();
    }

    public static double turtleActionRF() {
        return TURTLE_ACTION.get();
    }

    public static long turtleBufferRF() {
        return TURTLE_BUFFER.get();
    }

    public static long dockBufferRF() {
        return DOCK_BUFFER.get();
    }

    public static long dockPullRFPerTick() {
        return DOCK_PULL.get();
    }

    public static long dockChargeRFPerTick() {
        return DOCK_CHARGE.get();
    }

    // Portable devices
    public static CostMode pocketMode() { return POCKET_MODE.get(); }
    public static double pocketMultiplier() { return POCKET_MULTIPLIER.get(); }
    public static double pocketFixedRFPerTick() { return POCKET_FIXED.get(); }
    public static long pocketBufferRF() { return POCKET_BUFFER.get(); }
    public static double pocketPeripheralRFPerTick() { return POCKET_PERIPHERAL_TICK.get(); }
    public static double pocketPeripheralCallRF() { return POCKET_PERIPHERAL_CALL.get(); }
    public static boolean pocketUseComputerPeripherals() { return POCKET_USE_COMPUTER_PERIPHERALS.get(); }

    public static CostMode glassesMode() { return GLASSES_MODE.get(); }
    public static double glassesMultiplier() { return GLASSES_MULTIPLIER.get(); }
    public static double glassesFixedRFPerTick() { return GLASSES_FIXED.get(); }
    public static long glassesBufferRF() { return GLASSES_BUFFER.get(); }
    public static double glassesPeripheralRFPerTick() { return GLASSES_PERIPHERAL_TICK.get(); }
    public static double glassesPeripheralCallRF() { return GLASSES_PERIPHERAL_CALL.get(); }
    public static boolean glassesUseComputerPeripherals() { return GLASSES_USE_COMPUTER_PERIPHERALS.get(); }

    public static boolean loggingEnabled() {
        return LOGS.get();
    }

    public static boolean logStateTransitions() {
        return LOG_TRANSITIONS.get();
    }

    public static boolean logPeripheralDiscovery() {
        return LOG_DISCOVERY.get();
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC && loggingEnabled()) {
            CCPowerMod.LOG.info("CC: Power configuration reloaded");
        }
    }
}
