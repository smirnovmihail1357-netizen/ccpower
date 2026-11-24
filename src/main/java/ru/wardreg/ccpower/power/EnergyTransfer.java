package ru.wardreg.ccpower.power;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.wardreg.ccpower.dock.DockBlockEntity;
import ru.wardreg.ccpower.tesla.TeslaCoilBlockEntity;

/**
 * Common RF deposit/withdraw access through a CC peripheral. Any peripheral
 * whose target is one of our energy-holding blocks can be charged or drained
 * with the standard {@code receiveEnergy(amount)} / {@code extractEnergy(amount)}
 * methods, regardless of which mod provided the peripheral object.
 */
public final class EnergyTransfer {
    private EnergyTransfer() {}

    /** @return the RF stored in the peripheral's block, or 0 if it has none. */
    public static long stored(IPeripheral peripheral) {
        BlockEntity be = target(peripheral);
        if (be == null) return 0;
        if (be instanceof TeslaCoilBlockEntity coil) return coil.stored();
        if (be instanceof DockBlockEntity dock) return dock.getBuffer();
        APPeripheralState state = APPeripheralManager.stateAt(be);
        if (state != null) return state.stored();
        ComputerPowerState computer = computerState(be);
        return computer == null ? 0 : computer.bufferStored();
    }

    public static long capacity(IPeripheral peripheral) {
        BlockEntity be = target(peripheral);
        if (be == null) return 0;
        if (be instanceof TeslaCoilBlockEntity coil) return coil.capacity();
        if (be instanceof DockBlockEntity dock) return dock.getBufferCapacity();
        APPeripheralState state = APPeripheralManager.stateAt(be);
        if (state != null) return ru.wardreg.ccpower.CCPowerConfig.advancedPeripheralsBufferRF();
        return ru.wardreg.ccpower.CCPowerConfig.computerBufferRF();
    }

    /** Deposit RF into the block; returns the accepted amount. */
    public static long receive(IPeripheral peripheral, long amount) {
        BlockEntity be = target(peripheral);
        if (be == null || amount <= 0) return 0;
        if (be instanceof TeslaCoilBlockEntity coil) return coil.receive(amount);
        if (be instanceof DockBlockEntity dock) return dock.receiveRF(amount);
        APPeripheralState state = APPeripheralManager.stateAt(be);
        if (state != null) return state.receive(amount);
        ComputerPowerState computer = computerState(be);
        return computer == null ? 0 : computer.receiveBuffer(amount);
    }

    /** Withdraw RF from the block; returns the amount actually taken. */
    public static long extract(IPeripheral peripheral, long amount) {
        BlockEntity be = target(peripheral);
        if (be == null || amount <= 0) return 0;
        if (be instanceof TeslaCoilBlockEntity coil) return coil.extract(amount);
        if (be instanceof DockBlockEntity dock) return dock.extractRF(amount);
        APPeripheralState state = APPeripheralManager.stateAt(be);
        if (state != null && state.consume(amount)) return amount;
        ComputerPowerState computer = computerState(be);
        if (computer == null) return 0;
        return computer.consumeBuffer(amount) ? amount : 0;
    }

    private static BlockEntity target(IPeripheral peripheral) {
        if (peripheral == null) return null;
        Object target = peripheral.getTarget();
        return target instanceof BlockEntity be ? be : null;
    }

    private static ComputerPowerState computerState(BlockEntity be) {
        if (!(be instanceof AbstractComputerBlockEntity computer)) return null;
        int id = computer.getComputerID();
        return id < 0 ? null : PowerManager.state(id);
    }
}
