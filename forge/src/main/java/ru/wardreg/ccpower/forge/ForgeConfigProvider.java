package ru.wardreg.ccpower.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.platform.ConfigProvider;

/** Forge config: ForgeConfigSpec + ModConfigEvent-driven reload. */
public final class ForgeConfigProvider implements ConfigProvider {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue IDLE = BUILDER
        .comment("RF per game tick consumed by a powered-on computer that is idle")
        .defineInRange("power.idleRFPerTick", 2.0, 0.0, Double.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue ACTIVE = BUILDER
        .comment("Extra RF per game tick while a program is executing")
        .defineInRange("power.activeRFPerTick", 10.0, 0.0, Double.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue PERIPHERAL_TICK = BUILDER
        .comment("Default RF per game tick for a peripheral type without an explicit entry")
        .defineInRange("power.peripheralRFPerTick", 1.0, 0.0, Double.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue PERIPHERAL_CALL = BUILDER
        .comment("Base RF surcharge for a peripheral method call")
        .defineInRange("power.peripheralCallRF", 5.0, 0.0, Double.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue SURCHARGE_CAP = BUILDER
        .comment("Maximum total per-tick surcharge carried over from peripheral calls")
        .defineInRange("power.surchargeCapPerTick", 1000.0, 0.0, Double.MAX_VALUE);
    private static final ForgeConfigSpec.LongValue COMPUTER_BUFFER = BUILDER
        .comment("Capacity of the stationary computer's internal RF buffer")
        .defineInRange("power.computerBufferRF", 2_000L, 0L, Long.MAX_VALUE);
    private static final ForgeConfigSpec.LongValue COMPUTER_PULL = BUILDER
        .comment("RF per game tick a computer pulls from adjacent FE blocks")
        .defineInRange("power.computerPullRFPerTick", 1_000L, 0L, Long.MAX_VALUE);
    private static final ForgeConfigSpec.DoubleValue TURTLE_ACTION = BUILDER
        .comment("RF charged for every turtle action")
        .defineInRange("turtle.actionRF", 4.0, 0.0, Double.MAX_VALUE);
    private static final ForgeConfigSpec.LongValue TURTLE_BUFFER = BUILDER
        .comment("Capacity of the turtle internal RF battery")
        .defineInRange("turtle.bufferRF", 20_000L, 0L, Long.MAX_VALUE);
    private static final ForgeConfigSpec.LongValue DOCK_BUFFER = BUILDER
        .comment("Internal buffer of the Recharge Dock block, in RF")
        .defineInRange("dock.bufferRF", 50_000L, 0L, Long.MAX_VALUE);
    private static final ForgeConfigSpec.LongValue DOCK_PULL = BUILDER
        .comment("RF per game tick the Recharge Dock pulls from adjacent blocks")
        .defineInRange("dock.pullRFPerTick", 1000L, 0L, Long.MAX_VALUE);
    private static final ForgeConfigSpec.LongValue DOCK_CHARGE = BUILDER
        .comment("RF per game tick transferred into a turtle standing on the dock")
        .defineInRange("dock.chargeRFPerTick", 250L, 0L, Long.MAX_VALUE);
    private static final ForgeConfigSpec.BooleanValue LOGS = BUILDER
        .comment("Enable CC: Power log messages")
        .define("logging.enabled", true);
    private static final ForgeConfigSpec.BooleanValue LOG_TRANSITIONS = BUILDER
        .comment("Log discharge shutdowns and denied power-ons")
        .define("logging.stateTransitions", true);
    private static final ForgeConfigSpec.BooleanValue LOG_DISCOVERY = BUILDER
        .comment("Log discovered peripheral types")
        .define("logging.peripheralDiscovery", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public void onReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC && LOGS.get()) {
            CCPowerMod.LOG.info("CC: Power configuration reloaded");
        }
    }

    public double idleRFPerTick() { return IDLE.get(); }
    public double activeRFPerTick() { return ACTIVE.get(); }
    public double peripheralRFPerTick() { return PERIPHERAL_TICK.get(); }
    public double peripheralCallRF() { return PERIPHERAL_CALL.get(); }
    public double surchargeCapPerTick() { return SURCHARGE_CAP.get(); }
    public long computerBufferRF() { return COMPUTER_BUFFER.get(); }
    public long computerPullRFPerTick() { return COMPUTER_PULL.get(); }
    public double turtleActionRF() { return TURTLE_ACTION.get(); }
    public long turtleBufferRF() { return TURTLE_BUFFER.get(); }
    public long dockBufferRF() { return DOCK_BUFFER.get(); }
    public long dockPullRFPerTick() { return DOCK_PULL.get(); }
    public long dockChargeRFPerTick() { return DOCK_CHARGE.get(); }
    public boolean loggingEnabled() { return LOGS.get(); }
    public boolean logStateTransitions() { return LOG_TRANSITIONS.get(); }
    public boolean logPeripheralDiscovery() { return LOG_DISCOVERY.get(); }
}
