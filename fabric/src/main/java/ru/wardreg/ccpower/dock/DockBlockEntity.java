package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.wardreg.ccpower.power.CCPowerConfig;
import ru.wardreg.ccpower.power.TurtleEnergyView;
import ru.wardreg.ccpower.registry.ModRegistry;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Block entity of the Recharge Dock: pulls FE from neighbours, charges the turtle above. */
public class DockBlockEntity extends BlockEntity {
    private static final String TAG_BUFFER = "ccpower_rf";

    /** RF pulled from neighbours on the last server tick. */
    private long lastPull;

    /** RF transferred into a turtle on the last server tick. */
    private long lastCharge;

    /** Cached CC:T peripheral exposing the dock's state to Lua. */
    private final DockPeripheral peripheral = new DockPeripheral(this);

    private final SimpleEnergyStorage storage = new SimpleEnergyStorage(Long.MAX_VALUE, Long.MAX_VALUE, 0) {
        @Override
        public long getCapacity() { return CCPowerConfig.dockBufferRF(); }

        @Override
        public long insert(long amount, TransactionContext context) {
            long space = Math.max(0, CCPowerConfig.dockBufferRF() - this.amount);
            return super.insert(Math.min(amount, space), context);
        }

        @Override
        protected void onFinalCommit() { setChanged(); }
    };

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DOCK_BE, pos, state);
    }

    public EnergyStorage getStorage() { return storage; }
    public long getBuffer() { return storage.getAmount(); }
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
        long remaining = Math.min(CCPowerConfig.dockPullRFPerTick(), getBufferCapacity() - getBuffer());
        if (remaining <= 0) return;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            EnergyStorage neighbour = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null || !neighbour.supportsExtraction()) continue;
            long got;
            try (Transaction tx = Transaction.openOuter()) {
                got = neighbour.extract(Math.min(remaining, Integer.MAX_VALUE), tx);
                if (got > 0) tx.commit();
            }
            if (got > 0) {
                lastPull += got;
                remaining -= got;
            }
        }
    }

    private void tickChargeTurtle(Level level, BlockPos pos) {
        long stored = getBuffer();
        if (stored <= 0) return;
        BlockEntity above = level.getBlockEntity(pos.above());
        if (!(above instanceof TurtleBlockEntity turtleEntity)) return;
        ITurtleAccess access = turtleEntity.getAccess();
        if (!(access instanceof TurtleEnergyView turtle)) return;
        long transfer = Math.min(stored, CCPowerConfig.dockChargeRFPerTick());
        if (transfer <= 0) return;
        long accepted = turtle.ccpower$receiveEnergy(transfer);
        if (accepted > 0) {
            lastCharge += accepted;
            try (Transaction tx = Transaction.openOuter()) {
                storage.extract(accepted, tx);
                tx.commit();
            }
            setChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storage.amount = Math.max(0, tag.getLong(TAG_BUFFER));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(TAG_BUFFER, storage.getAmount());
    }
}
