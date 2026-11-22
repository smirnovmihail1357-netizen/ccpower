package ru.wardreg.ccpower.lua;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * The {@code ccpower} Lua API, available on every CC:Tweaked computer, turtle
 * and pocket computer. It reports the internal RF store and the energy flow
 * measured on the last completed server tick.
 *
 * <p>On CC:T 1.99.x (MC 1.17.1) an {@link ILuaAPI}'s own {@link LuaFunction}
 * methods are discovered directly by CC:T's method generator, so no generic
 * method source is needed here — unlike on newer CC:T versions. One instance
 * is created per computer through {@code ComputerCraftAPI.registerAPIFactory}.
 */
public final class PowerAPI implements ILuaAPI {
    /** Global Lua name of this API. */
    public static final String NAME = "ccpower";

    private final int id;

    public PowerAPI(int id) {
        this.id = id;
    }

    @Override
    public String[] getNames() {
        return new String[] {NAME};
    }

    /** RF currently held by the computer (turtle battery or PC buffer). */
    @LuaFunction("getEnergy")
    public MethodResult getEnergy() {
        ComputerPowerState state = PowerManager.stateOrNull(id);
        return MethodResult.of(state == null ? 0L : state.energyStored());
    }

    /** Capacity of the computer's internal RF store. */
    @LuaFunction("getCapacity")
    public MethodResult getCapacity() {
        ComputerPowerState state = PowerManager.stateOrNull(id);
        return MethodResult.of(state == null ? 0L : state.energyCapacity());
    }

    /** RF received on the last server tick (RF/tick). */
    @LuaFunction("getChargeRate")
    public MethodResult getChargeRate() {
        ComputerPowerState state = PowerManager.stateOrNull(id);
        return MethodResult.of(state == null ? 0L : state.chargeRate());
    }

    /** RF spent on the last server tick (RF/tick). */
    @LuaFunction("getDischargeRate")
    public MethodResult getDischargeRate() {
        ComputerPowerState state = PowerManager.stateOrNull(id);
        return MethodResult.of(state == null ? 0L : state.dischargeRate());
    }
}
