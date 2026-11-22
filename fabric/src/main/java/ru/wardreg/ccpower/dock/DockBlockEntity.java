package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.compat.FabricEnergy;
import ru.wardreg.ccpower.power.TurtleEnergyView;
import ru.wardreg.ccpower.registry.ModRegistry;
import team.reborn.energy.api.EnergyStorage;

/** Recharge Dock block entity: pulls RF from neighbours and feeds the turtle above. */
public class DockBlockEntity extends BlockEntity implements EnergyStorage {
    private static final String TAG_BUFFER = "ccpower_rf";
    private long buffer;

    /** RF pulled from neighbours on the last server tick. */
    private long lastPull;

    /** RF transferred into a turtle on the last server tick. */
    private long lastCharge;

    /** Cached CC:T peripheral exposing the dock's state to Lua. */
    private final DockPeripheral peripheral = new DockPeripheral(this);

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DOCK_BE, pos, state);
    }

    public EnergyStorage getStorage() { return this; }
    public long getBuffer() { return buffer; }
    public long getBufferCapacity() { return CCPowerConfig.dockBufferRF(); }

    /** @return RF/t pulled from FE neighbours on the last completed tick. */
    public long getPullRate() { return lastPull; }

    /** @return RF/t transferred into a turtle on the last completed tick. */
    public long getChargeRate() { return lastCharge; }

    /** @return the CC:T peripheral exposing this dock's internal state. */
    public DockPeripheral getPeripheral() { return peripheral; }

    // ---- Team Reborn Energy API ----
    @Override
    public boolean supportsInsertion() { return true; }

    @Override
    public long insert(long amount, TransactionContext tx) {
        if (amount <= 0) return 0;
        long accepted = Math.max(0, Math.min(amount, CCPowerConfig.dockBufferRF() - buffer));
        buffer += accepted;
        if (accepted > 0) setChanged();
        return accepted;
    }

    @Override
    public boolean supportsExtraction() { return false; }

    @Override
    public long extract(long amount, TransactionContext tx) { return 0; }

    @Override
    public long getAmount() { return buffer; }

    @Override
    public long getCapacity() { return CCPowerConfig.dockBufferRF(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DockBlockEntity dock) {
        dock.lastPull = 0;
        dock.lastCharge = 0;
        dock.tickPull(level, pos);
        dock.tickChargeTurtle(level, pos);
    }

    private void tickPull(Level level, BlockPos pos) {
        long remaining = Math.min(CCPowerConfig.dockPullRFPerTick(), CCPowerConfig.dockBufferRF() - buffer);
        if (remaining <= 0) return;
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            long got = FabricEnergy.extract(level, pos.relative(dir), dir.getOpposite(), remaining);
            if (got > 0) {
                buffer += got;
                lastPull += got;
                remaining -= got;
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    private void tickChargeTurtle(Level level, BlockPos pos) {
        if (buffer <= 0) return;
        BlockEntity above = level.getBlockEntity(pos.above());
        if (!(above instanceof TurtleBlockEntity turtleEntity)) return;
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
}
