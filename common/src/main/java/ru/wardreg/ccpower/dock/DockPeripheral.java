package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * CC:Tweaked peripheral for the Recharge Dock. Registered through the
 * loader-specific peripheral lookup (Forge provider / Fabric block API), so an
 * adjacent computer sees it as {@code peripheral.getType() == "ccpower_dock"}.
 *
 * <p>CC:T 1.105 discovers {@link LuaFunction} methods directly on an
 * {@link IPeripheral} implementation, so unlike newer CC:T ports no generic
 * method source is needed here.
 */
public final class DockPeripheral implements IPeripheral {
    /** Peripheral type name. */
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
    public Object getTarget() {
        return dock;
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof DockPeripheral peripheral && peripheral.dock == dock;
    }

    /** RF currently stored in the dock's internal buffer. */
    @LuaFunction("getEnergy")
    public final MethodResult getEnergy() {
        return MethodResult.of(dock.getBuffer());
    }

    /** Capacity of the dock's internal buffer. */
    @LuaFunction("getCapacity")
    public final MethodResult getCapacity() {
        return MethodResult.of(dock.getBufferCapacity());
    }

    /** RF/t pulled from adjacent FE storages on the last server tick. */
    @LuaFunction("getPullRate")
    public final MethodResult getPullRate() {
        return MethodResult.of(dock.getPullRate());
    }

    /** RF/t transferred into the turtle on the last server tick. */
    @LuaFunction("getChargeRate")
    public final MethodResult getChargeRate() {
        return MethodResult.of(dock.getChargeRate());
    }
}
