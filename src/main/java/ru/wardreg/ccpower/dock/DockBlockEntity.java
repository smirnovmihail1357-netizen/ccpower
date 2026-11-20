package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
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

/**
 * Block entity of the Recharge Dock. Every server tick it:
 * <ol>
 *   <li>pulls up to {@code dock.pullRFPerTick} RF from adjacent FE storages into its
 *       own buffer (the buffer is also exposed as a FE capability, so pipes can push);</li>
 *   <li>transfers up to {@code dock.chargeRFPerTick} RF into the internal battery of the
 *       turtle standing directly on top of the dock.</li>
 * </ol>
 */
public class DockBlockEntity extends BlockEntity {
    /** NBT key for the dock's internal buffer. */
    private static final String TAG_BUFFER = "ccpower_rf";

    /** Current RF buffer of the dock. Only touched on the server thread. */
    private long buffer;

    /** FE view of the dock's buffer, registered as a block capability. */
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
            // The dock never gives energy out through its capability; it only feeds turtles.
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

    private final LazyOptional<IEnergyStorage> storageCap = LazyOptional.of(() -> storage);

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DOCK_BE.get(), pos, state);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilityEnergy.ENERGY) return storageCap.cast();
        return super.getCapability(cap, side);
    }

    public IEnergyStorage getStorage() {
        return storage;
    }

    /** @return RF currently stored in the dock buffer (server side). */
    public long getBuffer() {
        return buffer;
    }

    /** @return configured capacity of the dock buffer. */
    public long getBufferCapacity() {
        return CCPowerConfig.dockBufferRF();
    }

    /** Server-side tick: pull RF from neighbours, charge the turtle above. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DockBlockEntity dock) {
        dock.tickPull(level, pos);
        dock.tickChargeTurtle(level, pos);
    }

    /** Pull RF from adjacent blocks into the dock buffer. */
    private void tickPull(Level level, BlockPos pos) {
        long capacity = CCPowerConfig.dockBufferRF();
        long remaining = Math.min(CCPowerConfig.dockPullRFPerTick(), capacity - buffer);
        if (remaining <= 0) return;

        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            BlockEntity neighbourBe = level.getBlockEntity(pos.relative(dir));
            IEnergyStorage neighbour = neighbourBe == null ? null : neighbourBe
                .getCapability(CapabilityEnergy.ENERGY, dir.getOpposite())
                .resolve().orElse(null);
            if (neighbour == null || !neighbour.canExtract()) continue;
            int extracted = neighbour.extractEnergy((int) Math.min(remaining, Integer.MAX_VALUE), false);
            if (extracted > 0) {
                buffer += extracted;
                remaining -= extracted;
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    /** Feed the turtle directly above from the dock buffer. */
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
            setChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer = Math.max(0, tag.getLong(TAG_BUFFER));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        super.save(tag);
        tag.putLong(TAG_BUFFER, buffer);
        return tag;
    }
}
