package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.LinkedHashMap;
import java.util.Map;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;
import ru.wardreg.ccpower.power.TurtleEnergyView;

/**
 * CC:Tweaked peripheral for the Recharge Dock. Registered through the
 * standard {@code PeripheralCapability}, so an adjacent computer sees it as
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
    public MethodResult getEnergy(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getBuffer());
    }

    /** Capacity of the dock's internal buffer. */
    @LuaFunction("getCapacity")
    public MethodResult getCapacity(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getBufferCapacity());
    }

    /** RF/t pulled from adjacent FE storages on the last server tick. */
    @LuaFunction("getPullRate")
    public MethodResult getPullRate(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getPullRate());
    }

    /** RF/t transferred into the turtle on the last server tick. */
    @LuaFunction("getChargeRate")
    public MethodResult getChargeRate(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock == null ? 0L : peripheral.dock.getChargeRate());
    }

    /** Whether a turtle is currently standing on the dock. */
    @LuaFunction("hasTurtle")
    public MethodResult hasTurtle(DockPeripheral peripheral) {
        return MethodResult.of(peripheral.dock != null && peripheral.dock.getTurtleOnTop() != null);
    }

    /**
     * Information about the turtle parked on the dock, or {@code nil} when the
     * dock is empty. The returned table has {@code id}, {@code energy},
     * {@code capacity}, {@code chargeRate} and {@code dischargeRate} fields.
     */
    @LuaFunction("getTurtleInfo")
    public MethodResult getTurtleInfo(DockPeripheral peripheral) {
        if (peripheral.dock == null) return MethodResult.of();
        TurtleEnergyView turtle = peripheral.dock.getTurtleOnTop();
        if (turtle == null) return MethodResult.of();

        int id = turtle.ccpower$computerId();
        ComputerPowerState state = PowerManager.stateOrNull(id);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", id);
        info.put("energy", turtle.ccpower$getEnergy());
        info.put("capacity", turtle.ccpower$getCapacity());
        info.put("chargeRate", state == null ? 0L : state.chargeRate());
        info.put("dischargeRate", state == null ? 0L : state.dischargeRate());
        return MethodResult.of(info);
    }
}
