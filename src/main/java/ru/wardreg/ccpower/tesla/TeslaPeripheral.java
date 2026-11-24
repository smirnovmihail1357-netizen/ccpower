package ru.wardreg.ccpower.tesla;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;

/** CC:T peripheral for a Tesla Coil (type {@code ccpower_tesla_coil}). */
public final class TeslaPeripheral implements IPeripheral, GenericSource {
    public static final String TYPE = "ccpower_tesla_coil";

    private final TeslaCoilBlockEntity coil;

    public TeslaPeripheral(TeslaCoilBlockEntity coil) {
        this.coil = coil;
    }

    /** Source-only instance used to register Lua methods. */
    public TeslaPeripheral() {
        this(null);
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public String id() { return TYPE; }

    @Override
    public Object getTarget() { return coil; }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof TeslaPeripheral p && p.coil == coil;
    }

    /** RF currently stored in the coil's internal buffer. */
    @LuaFunction("getEnergy")
    public MethodResult getEnergy(TeslaPeripheral peripheral) {
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.stored());
    }

    /** Capacity of the internal buffer. */
    @LuaFunction("getCapacity")
    public MethodResult getCapacity(TeslaPeripheral peripheral) {
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.capacity());
    }

    /** Total RF the coil has given away since it was built. */
    @LuaFunction("getEnergyGiven")
    public MethodResult getEnergyGiven(TeslaPeripheral peripheral) {
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.totalGiven());
    }

    /** Number of pulses that actually delivered energy. */
    @LuaFunction("getPulseCount")
    public MethodResult getPulseCount(TeslaPeripheral peripheral) {
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.pulseCount());
    }

    /** RF delivered by the last pulse. */
    @LuaFunction("getLastPulseEnergy")
    public MethodResult getLastPulseEnergy(TeslaPeripheral peripheral) {
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.lastPulseEnergy());
    }

    /** Average RF/second delivered over the coil's lifetime. */
    @LuaFunction("getDeliveryRate")
    public MethodResult getDeliveryRate(TeslaPeripheral peripheral) {
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.deliveryRate());
    }

    /** Push RF into the internal buffer; returns the accepted amount. */
    @LuaFunction("receiveEnergy")
    public MethodResult receiveEnergy(TeslaPeripheral peripheral, IArguments args) throws Exception {
        long amount = args.getLong(0);
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.receive(amount));
    }

    /** Pull RF out of the internal buffer; returns the amount taken. */
    @LuaFunction("extractEnergy")
    public MethodResult extractEnergy(TeslaPeripheral peripheral, IArguments args) throws Exception {
        long amount = args.getLong(0);
        return MethodResult.of(peripheral.coil == null ? 0L : peripheral.coil.extract(amount));
    }
}
