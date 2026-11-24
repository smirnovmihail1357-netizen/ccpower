package ru.wardreg.ccpower.power;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * Adds generic RF read/transfer methods to every CC peripheral whose target is
 * one of our energy-holding blocks: {@code getEnergy()}, {@code getCapacity()},
 * {@code receiveEnergy(amount)} and {@code extractEnergy(amount)}.
 *
 * <p>The target is an {@link IPeripheral} interface, so this also covers
 * peripherals provided by third-party mods (for example Advanced Peripherals
 * blocks) without any compile-time dependency on them.
 */
public final class UniversalEnergyPeripheral implements GenericSource {
    @Override
    public String id() {
        return "ccpower_universal_energy";
    }

    @LuaFunction("getEnergy")
    public MethodResult getEnergy(IPeripheral peripheral) {
        return MethodResult.of(EnergyTransfer.stored(peripheral));
    }

    @LuaFunction("getCapacity")
    public MethodResult getCapacity(IPeripheral peripheral) {
        return MethodResult.of(EnergyTransfer.capacity(peripheral));
    }

    @LuaFunction("receiveEnergy")
    public MethodResult receiveEnergy(IPeripheral peripheral, IArguments args) throws LuaException {
        return MethodResult.of(EnergyTransfer.receive(peripheral, args.getLong(0)));
    }

    @LuaFunction("extractEnergy")
    public MethodResult extractEnergy(IPeripheral peripheral, IArguments args) throws LuaException {
        return MethodResult.of(EnergyTransfer.extract(peripheral, args.getLong(0)));
    }
}
