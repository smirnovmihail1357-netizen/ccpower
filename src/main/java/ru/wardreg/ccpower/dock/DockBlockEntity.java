package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.Capabilities;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.TurtleEnergyView;
import ru.wardreg.ccpower.registry.ModRegistry;

/** Block entity of the Recharge Dock: pulls FE from neighbours, charges the turtle above. */
public class DockBlockEntity extends BlockEntity {
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
        public int extractEnergy(int toExtract, boolean simulate) { return 0; }

        @Override
        public int getEnergyStored() { return (int) Math.min(buffer, Integer.MAX_VALUE); }

        @Override
        public int getMaxEnergyStored() { return (int) Math.min(CCPowerConfig.dockBufferRF(), Integer.MAX_VALUE); }

        @Override
        public boolean canExtract() { return false; }

        @Override
        public boolean canReceive() { return true; }
    };

    private final LazyOptional<IEnergyStorage> storageCap = LazyOptional.of(() -> storage);

    /** CC:T peripheral capability exposing the dock's internal state. */
    private final LazyOptional<DockPeripheral> peripheralCap = LazyOptional.of(() -> peripheral);

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DOCK_BE.get(), pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilityEnergy.ENERGY) return (LazyOptional<T>) storageCap;
        if (cap == Capabilities.CAPABILITY_PERIPHERAL) return peripheralCap.cast();
        return super.getCapability(cap, side);
    }

    public IEnergyStorage getStorage() { return storage; }
    public long getBuffer() { return buffer; }
    public long getBufferCapacity() { return CCPowerConfig.dockBufferRF(); }

    /** @return RF/t pulled from FE neighbours on the last completed tick. */
    public long getPullRate() { return lastPull; }

    /** @return RF/t transferred into a turtle on the last completed tick. */
    public long getChargeRate() { return lastCharge; }

    /** @return the CC:T peripheral exposing this dock's internal state. */
    public DockPeripheral getPeripheral() { return peripheral; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DockBlockEntity dock) {
        dock.lastPull = 0;
        dock.lastCharge = 0;
        dock.tickPull(level, pos);
        dock.tickChargeTurtle(level, pos);
    }

    private void tickPull(Level level, BlockPos pos) {
        long capacity = CCPowerConfig.dockBufferRF();
        long remaining = Math.min(CCPowerConfig.dockPullRFPerTick(), capacity - buffer);
        if (remaining <= 0) return;
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            BlockEntity neighbourBe = level.getBlockEntity(pos.relative(dir));
            if (neighbourBe == null) continue;
            IEnergyStorage neighbour = neighbourBe.getCapability(CapabilityEnergy.ENERGY, dir.getOpposite()).orElse(null);
            if (neighbour == null || !neighbour.canExtract()) continue;
            int extracted = neighbour.extractEnergy((int) Math.min(remaining, Integer.MAX_VALUE), false);
            if (extracted > 0) {
                buffer += extracted;
                lastPull += extracted;
                remaining -= extracted;
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    private void tickChargeTurtle(Level level, BlockPos pos) {
        if (buffer <= 0) return;
        BlockEntity above = level.getBlockEntity(pos.above());
        if (!(above instanceof TileTurtle turtleEntity)) return;
        ITurtleAccess access = turtleEntity.getAccess();
        if (!(access instanceof TurtleEnergyView turtle)) return;
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
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer = Math.max(0, tag.getLong(TAG_BUFFER));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(TAG_BUFFER, buffer);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        storageCap.invalidate();
        peripheralCap.invalidate();
    }
}
