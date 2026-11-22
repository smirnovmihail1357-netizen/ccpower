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
 * <p>One instance is created per computer by CC:T's {@code ILuaAPIFactory}.
 * Methods are ordinary final instance methods: CC:T 1.105 discovers Lua
 * functions directly on an {@link ILuaAPI} implementation (the generic-source
 * API of newer CC:T versions is not needed here).
 */
public final class PowerAPI implements ILuaAPI {
    private final int id;

    public PowerAPI(int id) {
        this.id = id;
    }

    @Override
    public String[] getNames() {
        return new String[] {"ccpower"};
    }

    /** RF currently held by the computer (turtle battery or PC buffer). */
    @LuaFunction("getEnergy")
    public final MethodResult getEnergy() {
        return MethodResult.of(value(ComputerPowerState::energyStored));
    }

    /** Capacity of the computer's internal RF store. */
    @LuaFunction("getCapacity")
    public final MethodResult getCapacity() {
        return MethodResult.of(value(ComputerPowerState::energyCapacity));
    }

    /** RF received on the last server tick (RF/tick). */
    @LuaFunction("getChargeRate")
    public final MethodResult getChargeRate() {
        return MethodResult.of(value(ComputerPowerState::chargeRate));
    }

    /** RF spent on the last server tick (RF/tick). */
    @LuaFunction("getDischargeRate")
    public final MethodResult getDischargeRate() {
        return MethodResult.of(value(ComputerPowerState::dischargeRate));
    }

    private long value(java.util.function.ToLongFunction<ComputerPowerState> getter) {
        ComputerPowerState state = PowerManager.stateOrNull(id);
        return state == null ? 0L : getter.applyAsLong(state);
    }
}
