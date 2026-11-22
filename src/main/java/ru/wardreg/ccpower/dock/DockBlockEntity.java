package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.TurtleEnergyView;
import ru.wardreg.ccpower.registry.ModRegistry;

/** Block entity of the Recharge Dock (1.15.2 TileEntity era). */
public class DockBlockEntity extends TileEntity implements ITickableTileEntity {
    private static final String TAG_BUFFER = "ccpower_rf";

    private long buffer;

    /** RF pulled from neighbours on the last server tick. */
    private long lastPull;

    /** RF transferred into a turtle on the last server tick. */
    private long lastCharge;

    /** Cached CC:T peripheral exposing the dock's state to Lua. */
    private final DockPeripheral peripheral = new DockPeripheral(this);

    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            long space = CCPowerConfig.dockBufferRF() - buffer;
            long accepted = Math.max(0, Math.min(toReceive, space));
            if (!simulate) {
                buffer += accepted;
                if (accepted > 0) setChanged();
            }
            return (int) accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(buffer, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(CCPowerConfig.dockBufferRF(), Integer.MAX_VALUE);
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> storage);

    public DockBlockEntity() {
        super(ModRegistry.DOCK_BE.get());
    }

    public IEnergyStorage getStorage() {
        return storage;
    }

    public long getBuffer() {
        return buffer;
    }

    public long getBufferCapacity() {
        return CCPowerConfig.dockBufferRF();
    }

    /** @return RF/t pulled from FE neighbours on the last completed tick. */
    public long getPullRate() {
        return lastPull;
    }

    /** @return RF/t transferred into a turtle on the last completed tick. */
    public long getChargeRate() {
        return lastCharge;
    }

    /** @return the CC:T peripheral exposing this dock's internal state. */
    public DockPeripheral getPeripheral() {
        return peripheral;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilityEnergy.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) return;
        lastPull = 0;
        lastCharge = 0;
        tickPull();
        tickChargeTurtle();
    }

    private void tickPull() {
        long capacity = CCPowerConfig.dockBufferRF();
        long remaining = Math.min(CCPowerConfig.dockPullRFPerTick(), capacity - buffer);
        if (remaining <= 0) return;

        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            TileEntity neighbour = level.getBlockEntity(getBlockPos().relative(dir));
            if (neighbour == null) continue;
            IEnergyStorage storage = neighbour
                .getCapability(CapabilityEnergy.ENERGY, dir.getOpposite()).orElse(null);
            if (storage == null || !storage.canExtract()) continue;
            int extracted = storage.extractEnergy((int) Math.min(remaining, Integer.MAX_VALUE), false);
            if (extracted > 0) {
                buffer += extracted;
                lastPull += extracted;
                remaining -= extracted;
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    private void tickChargeTurtle() {
        if (buffer <= 0) return;
        TileEntity above = level.getBlockEntity(getBlockPos().above());
        if (!(above instanceof TileTurtle)) return;

        ITurtleAccess access = ((TileTurtle) above).getAccess();
        if (!(access instanceof TurtleEnergyView)) return;

        TurtleEnergyView turtle = (TurtleEnergyView) access;
        long transfer = Math.min(buffer, CCPowerConfig.dockChargeRFPerTick());
        if (transfer <= 0) return;
        long accepted = turtle.ccpower$receiveEnergy(transfer);
        if (accepted > 0) {
            buffer -= accepted;
            lastCharge += accepted;
            setChanged();
        }
    }

    @Override
    public void load(CompoundNBT tag) {
        super.load(tag);
        buffer = Math.max(0, tag.getLong(TAG_BUFFER));
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putLong(TAG_BUFFER, buffer);
        return tag;
    }
}
