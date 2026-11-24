package ru.wardreg.ccpower.power;

import ru.wardreg.ccpower.CCPowerConfig;

/**
 * Per-device cost calculation for pocket computers and Smart Glasses.
 *
 * <p>Two mutually exclusive modes are supported through {@link CostMode}:
 * a multiplier applied to the normal computer's cost formula, or one fixed
 * RF/tick value. Peripheral prices can come either from the normal computer's
 * per-type table or from the device's own flat values.
 */
public final class PortableCosts {
    private PortableCosts() {}

    /** Capacity of the device's internal RF buffer. */
    public static long capacity(DeviceKind kind) {
        return kind == DeviceKind.POCKET
            ? CCPowerConfig.pocketBufferRF()
            : CCPowerConfig.glassesBufferRF();
    }

    /** RF/t a peripheral attached to this device costs every tick. */
    private static double peripheralTickCost(DeviceKind kind, ComputerPowerState state) {
        boolean useComputer = kind == DeviceKind.POCKET
            ? CCPowerConfig.pocketUseComputerPeripherals()
            : CCPowerConfig.glassesUseComputerPeripherals();
        if (useComputer) {
            double total = 0.0;
            for (String type : state.peripheralTypes()) {
                total += PeripheralCosts.tickCost(type);
            }
            return total;
        }
        double per = kind == DeviceKind.POCKET
            ? CCPowerConfig.pocketPeripheralRFPerTick()
            : CCPowerConfig.glassesPeripheralRFPerTick();
        return per * state.peripheralTypes().size();
    }

    /** RF charged for one method call on a peripheral of the given type. */
    public static double callCost(DeviceKind kind, String type) {
        boolean useComputer = kind == DeviceKind.POCKET
            ? CCPowerConfig.pocketUseComputerPeripherals()
            : CCPowerConfig.glassesUseComputerPeripherals();
        if (useComputer) return PeripheralCosts.callCost(type);
        return kind == DeviceKind.POCKET
            ? CCPowerConfig.pocketPeripheralCallRF()
            : CCPowerConfig.glassesPeripheralCallRF();
    }

    /**
     * Total RF per tick for a portable device.
     *
     * @param surcharge accumulated peripheral call surcharges, already drained
     */
    public static double tickCost(DeviceKind kind, ComputerPowerState state,
                                  boolean active, double surcharge) {
        CostMode mode = kind == DeviceKind.POCKET
            ? CCPowerConfig.pocketMode()
            : CCPowerConfig.glassesMode();

        if (mode == CostMode.FIXED) {
            return kind == DeviceKind.POCKET
                ? CCPowerConfig.pocketFixedRFPerTick()
                : CCPowerConfig.glassesFixedRFPerTick();
        }

        double base = CCPowerConfig.idleRFPerTick()
            + (active ? CCPowerConfig.activeRFPerTick() : 0.0)
            + peripheralTickCost(kind, state)
            + surcharge;
        double multiplier = kind == DeviceKind.POCKET
            ? CCPowerConfig.pocketMultiplier()
            : CCPowerConfig.glassesMultiplier();
        return base * multiplier;
    }
}
