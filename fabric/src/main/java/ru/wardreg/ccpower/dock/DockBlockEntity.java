package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.TurtleEnergyView;
import ru.wardreg.ccpower.registry.ModRegistry;
import team.reborn.energy.api.EnergyStorage;

/**
 * Block entity of the Recharge Dock. Every server tick it pulls RF from
 * adjacent FE storages into its own buffer and feeds the turtle standing on top.
 *
 * <p>Fabric build: the exposed FE capability is a Team Reborn Energy storage.
 */
public class DockBlockEntity extends BlockEntity {
    private static final String TAG_BUFFER = "ccpower_rf";

    private long buffer;

    private final EnergyStorage storage = new DockEnergyStorage();

    /** Transaction-aware view of the dock buffer. */
    private final class DockEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
        @Override
        protected Long createSnapshot() {
            return buffer;
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            buffer = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }

        @Override
        public boolean supportsInsertion() {
            return true;
        }

        @Override
        public long insert(long amount, TransactionContext transaction) {
            if (amount <= 0) return 0;
            long space = Math.max(0, CCPowerConfig.dockBufferRF() - buffer);
            long accepted = Math.min(amount, space);
            if (accepted > 0) {
                updateSnapshots(transaction);
                buffer += accepted;
            }
            return accepted;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public long extract(long amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmount() {
            return buffer;
        }

        @Override
        public long getCapacity() {
            return CCPowerConfig.dockBufferRF();
        }
    }

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DOCK_BE, pos, state);
    }

    public EnergyStorage getStorage() {
        return storage;
    }

    public long getBuffer() {
        return buffer;
    }

    public long getBufferCapacity() {
        return CCPowerConfig.dockBufferRF();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DockBlockEntity dock) {
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
            EnergyStorage neighbour =
                EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null || !neighbour.supportsExtraction()) continue;
            try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long got = neighbour.extract(remaining, tx);
                if (got > 0) {
                    tx.commit();
                    buffer += got;
                    remaining -= got;
                    changed = true;
                }
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
            setChanged();
        }
    }

    @Override
    public void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        buffer = Math.max(0, tag.getLongOr(TAG_BUFFER, 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putLong(TAG_BUFFER, buffer);
    }
}
