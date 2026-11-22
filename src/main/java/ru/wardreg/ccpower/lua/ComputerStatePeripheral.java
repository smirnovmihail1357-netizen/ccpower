package ru.wardreg.ccpower.lua;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.shared.computer.blocks.ComputerPeripheral;
import net.minecraft.resources.ResourceLocation;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Adds CC: Power state methods to <em>every</em> CC:T computer peripheral.
 *
 * <p>CC:T exposes both computers and turtles to neighbouring computers as an
 * {@code IPeripheral} of type {@code computer} / {@code turtle}, implemented by
 * {@link ComputerPeripheral}. On CC:T 1.99.x a {@link GenericSource} must expose
 * <em>static</em> {@link LuaFunction} methods whose first argument is the target
 * class; CC:T then attaches them to every peripheral of that class.
 *
 * <p>Usage from another computer:
 * <pre>{@code
 * local p = peripheral.wrap("back")
 * p.getEnergy(), p.getCapacity(), p.getChargeRate(), p.getDischargeRate()
 * }</pre>
 */
public final class ComputerStatePeripheral implements GenericSource {
    /** Generic method source id (not the Lua peripheral type). */
    public static final ResourceLocation SOURCE_ID =
        new ResourceLocation("ccpower", "ccpower_computer_state");

    public ComputerStatePeripheral() {}

    @Override
    public ResourceLocation id() {
        return SOURCE_ID;
    }

    private static ComputerPowerState state(ComputerPeripheral peripheral) {
        return peripheral == null ? null : PowerManager.stateOrNull(peripheral.getID());
    }

    /** RF currently held by the remote computer (buffer or turtle battery). */
    @LuaFunction("getEnergy")
    public static MethodResult getEnergy(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.energyStored());
    }

    /** Capacity of the remote computer's RF store. */
    @LuaFunction("getCapacity")
    public static MethodResult getCapacity(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.energyCapacity());
    }

    /** RF/t received by the remote computer on its last server tick. */
    @LuaFunction("getChargeRate")
    public static MethodResult getChargeRate(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.chargeRate());
    }

    /** RF/t spent by the remote computer on its last server tick. */
    @LuaFunction("getDischargeRate")
    public static MethodResult getDischargeRate(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.dischargeRate());
    }
}
