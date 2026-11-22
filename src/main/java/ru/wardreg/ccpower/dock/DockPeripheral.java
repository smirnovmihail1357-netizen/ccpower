package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * CC:Tweaked peripheral for the Recharge Dock. On MC 1.17.1 / CC:T 1.99.x the
 * dock is exposed through {@code ComputerCraftAPI.registerPeripheralProvider},
 * so an adjacent computer sees it as {@code peripheral.getType() == "ccpower_dock"}.
 *
 * <p>Its {@link LuaFunction} methods are instance methods: CC:T 1.99.x
 * discovers the peripheral object's own methods directly (no generic source
 * needed, unlike on newer CC:T versions). One instance is created per dock.
 */
public final class DockPeripheral implements IPeripheral {
    /** Lua peripheral type name. */
    public static final String TYPE = "ccpower_dock";

    private final DockBlockEntity dock;

    public DockPeripheral(DockBlockEntity dock) {
        this.dock = dock;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof DockPeripheral peripheral && peripheral.dock == dock;
    }

    /** RF currently stored in the dock's internal buffer. */
    @LuaFunction("getEnergy")
    public MethodResult getEnergy() {
        return MethodResult.of(dock.getBuffer());
    }

    /** Capacity of the dock's internal buffer. */
    @LuaFunction("getCapacity")
    public MethodResult getCapacity() {
        return MethodResult.of(dock.getBufferCapacity());
    }

    /** RF/t pulled from adjacent FE storages on the last server tick. */
    @LuaFunction("getPullRate")
    public MethodResult getPullRate() {
        return MethodResult.of(dock.getPullRate());
    }

    /** RF/t transferred into the turtle on the last server tick. */
    @LuaFunction("getChargeRate")
    public MethodResult getChargeRate() {
        return MethodResult.of(dock.getChargeRate());
    }
}
