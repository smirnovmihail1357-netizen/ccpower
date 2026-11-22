package ru.wardreg.ccpower.lua;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import net.minecraft.util.ResourceLocation;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * The {@code ccpower} Lua API, available on every CC:Tweaked computer, turtle
 * and pocket computer. Reports the internal RF store and the energy flow
 * measured on the last completed server tick.
 *
 * <p>Forge/CC:T 1.101 adaptation: this class is both an {@link ILuaAPI} (one
 * instance per computer, created by CC:T's {@code ILuaAPIFactory}) and a
 * {@link GenericSource} (one instance registered at startup). In this CC:T
 * version {@code GenericSource.id()} returns a {@link ResourceLocation}, not a
 * String, and {@code ILuaAPIFactory.create} receives an {@code IComputerSystem}
 * whose {@code getID()} is inherited from {@code IComputerAccess}.
 */
public final class PowerAPI implements ILuaAPI, GenericSource {
    /** Lua API name and generic method source path. */
    public static final String SOURCE_ID = "ccpower";
    private static final ResourceLocation ID = new ResourceLocation(CCPowerMod.MODID, SOURCE_ID);

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
        return new String[] {SOURCE_ID};
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /** RF currently held by the computer (turtle battery or PC buffer). */
    @LuaFunction("getEnergy")
    public MethodResult getEnergy(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.energyStored());
    }

    /** Capacity of the computer's internal RF store. */
    @LuaFunction("getCapacity")
    public MethodResult getCapacity(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.energyCapacity());
    }

    /** RF received on the last server tick (RF/tick). */
    @LuaFunction("getChargeRate")
    public MethodResult getChargeRate(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.chargeRate());
    }

    /** RF spent on the last server tick (RF/tick). */
    @LuaFunction("getDischargeRate")
    public MethodResult getDischargeRate(PowerAPI api) {
        ComputerPowerState state = PowerManager.stateOrNull(api.id);
        return MethodResult.of(state == null ? 0L : state.dischargeRate());
    }
}
