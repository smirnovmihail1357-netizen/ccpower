package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * CC:Tweaked peripheral for the Recharge Dock. Registered through the
 * loader's standard peripheral lookup, so an adjacent computer sees it as
 * {@code peripheral.getType() == "ccpower_dock"}.
 *
 * <p>Like {@code PowerAPI}, this object is both an {@link IPeripheral} and a
 * {@link GenericSource}: the latter makes CC:T discover the {@link LuaFunction}
 * methods below. One instance is created per dock; a second, dock-less
 * instance is registered once as the method source.
 */
public final class DockPeripheral implements IPeripheral, GenericSource {
    /** Peripheral type name and generic method source id. */
    public static final String TYPE = "ccpower_dock";

    private final DockBlockEntity dock;

    public DockPeripheral(DockBlockEntity dock) {
        this.dock = dock;
    }

    /** Source-only instance used at startup to register the Lua methods. */
    public DockPeripheral() {
        this(null);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String id() {
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
    public static MethodResult getEnergy(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getBuffer());
    }

    /** Capacity of the dock's internal buffer. */
    @LuaFunction("getCapacity")
    public static MethodResult getCapacity(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getBufferCapacity());
    }

    /** RF/t pulled from adjacent FE storages on the last server tick. */
    @LuaFunction("getPullRate")
    public static MethodResult getPullRate(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getPullRate());
    }

    /** RF/t transferred into the turtle on the last server tick. */
    @LuaFunction("getChargeRate")
    public static MethodResult getChargeRate(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getChargeRate());
    }
}
