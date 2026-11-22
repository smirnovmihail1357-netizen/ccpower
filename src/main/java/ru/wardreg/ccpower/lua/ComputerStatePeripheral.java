package ru.wardreg.ccpower.lua;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.shared.computer.blocks.ComputerPeripheral;
import net.minecraft.util.ResourceLocation;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Adds CC: Power state methods to every CC:T computer peripheral.
 *
 * <p>CC:T exposes both computers and turtles to neighbouring computers as an
 * {@code IPeripheral} of type {@code computer} / {@code turtle}, implemented by
 * {@link ComputerPeripheral}. Declaring generic methods whose first argument
 * is {@code ComputerPeripheral} makes them appear on every such peripheral.
 */
public final class ComputerStatePeripheral implements GenericSource {
    /** Generic method source path (not the Lua peripheral type). */
    public static final String SOURCE_ID = "ccpower_computer_state";
    private static final ResourceLocation ID =
        new ResourceLocation(CCPowerMod.MODID, SOURCE_ID);

    @Override
    public ResourceLocation id() {
        return ID;
    }

    private static ComputerPowerState state(ComputerPeripheral peripheral) {
        return peripheral == null ? null : PowerManager.stateOrNull(peripheral.getID());
    }

    /** RF currently held by the remote computer (buffer or turtle battery). */
    @LuaFunction("getEnergy")
    public MethodResult getEnergy(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.energyStored());
    }

    /** Capacity of the remote computer's RF store. */
    @LuaFunction("getCapacity")
    public MethodResult getCapacity(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.energyCapacity());
    }

    /** RF/t received by the remote computer on its last server tick. */
    @LuaFunction("getChargeRate")
    public MethodResult getChargeRate(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.chargeRate());
    }

    /** RF/t spent by the remote computer on its last server tick. */
    @LuaFunction("getDischargeRate")
    public MethodResult getDischargeRate(ComputerPeripheral peripheral) {
        ComputerPowerState state = state(peripheral);
        return MethodResult.of(state == null ? 0L : state.dischargeRate());
    }
}
