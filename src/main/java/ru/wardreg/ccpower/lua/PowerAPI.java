package ru.wardreg.ccpower.lua;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import net.minecraft.util.ResourceLocation;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * The {@code ccpower} Lua API, available on every CC:Tweaked computer, turtle
 * and pocket computer. It reports the internal RF store and the energy flow
 * measured on the last completed server tick.
 *
 * <p>This class is both an {@link ILuaAPI} (one instance per computer, created
 * by CC:T's {@code ILuaAPIFactory}) and a {@link GenericSource} (one instance
 * registered with CC:T at startup). The latter makes CC:T's method supplier
 * pick up the {@link LuaFunction} methods below, which otherwise would not be
 * exposed to Lua. The first argument of every method is the API object itself
 * - the convention CC:T uses for generic methods (see its own EnergyMethods).
 *
 * <p>Era note: CC:T 1.97 requires generic source methods to be {@code static}
 * and {@code GenericSource.id()} to return a {@link ResourceLocation}.
 */
public final class PowerAPI implements ILuaAPI, GenericSource {
    /** Source id used by CC:T's generic method supplier. */
    public static final String SOURCE_ID = "ccpower";

    private final int id;

    /** Instance used only to register the generic method source. */
    public PowerAPI() {
        this(-1);
    }

    public PowerAPI(int id) {
        this.id = id;
    }

    @Override
    public String[] getNames() {
        return new String[] {"ccpower"};
    }

    @Override
    public ResourceLocation id() {
        return new ResourceLocation(SOURCE_ID);
    }

    /** RF currently held by the computer (turtle battery or PC buffer). */
    @LuaFunction("getEnergy")
    public static MethodResult getEnergy(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.energyStored());
    }

    /** Capacity of the computer's internal RF store. */
    @LuaFunction("getCapacity")
    public static MethodResult getCapacity(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.energyCapacity());
    }

    /** RF received on the last server tick (RF/tick). */
    @LuaFunction("getChargeRate")
    public static MethodResult getChargeRate(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.chargeRate());
    }

    /** RF spent on the last server tick (RF/tick). */
    @LuaFunction("getDischargeRate")
    public static MethodResult getDischargeRate(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.dischargeRate());
    }
}
