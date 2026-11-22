package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.TurtleEnergyView;
import ru.wardreg.ccpower.registry.ModRegistry;
import team.reborn.energy.api.EnergyStorage;

/**
 * Block entity of the Recharge Dock. Every server tick it pulls RF from
 * adjacent Team Reborn Energy storages into its own buffer and charges the
 * turtle standing on top. The buffer is also exposed as an EnergyStorage so
 * Fabric cables can push energy into it.
 */
public class DockBlockEntity extends BlockEntity {
    private static final String TAG_BUFFER = "ccpower_rf";

    /** Current RF buffer of the dock. Only touched on the server thread. */
    private long buffer;

    /** RF pulled from neighbours on the last server tick. */
    private long lastPull;

    /** RF transferred into a turtle on the last server tick. */
    private long lastCharge;

    /** Cached CC:T peripheral exposing the dock's state to Lua. */
    private final DockPeripheral peripheral = new DockPeripheral(this);

    private final EnergyStorage storage = new EnergyStorage() {
        @Override
        public long insert(long amount, TransactionContext transaction) {
            long space = CCPowerConfig.dockBufferRF() - buffer;
            long accepted = Math.max(0, Math.min(amount, space));
            if (accepted > 0) setChanged();
            buffer += accepted;
            return accepted;
        }

        @Override
        public long extract(long amount, TransactionContext transaction) {
            return 0; // The dock only feeds turtles, never exposes energy outward.
        }

        @Override
        public long getAmount() {
            return buffer;
        }

        @Override
        public long getCapacity() {
            return CCPowerConfig.dockBufferRF();
        }

        @Override
        public boolean supportsInsertion() {
            return true;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }
    };

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
            EnergyStorage neighbour = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (neighbour == null || !neighbour.supportsExtraction()) continue;
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long extracted = neighbour.extract(remaining, transaction);
                transaction.commit();
                if (extracted > 0) {
                    buffer += extracted;
                    lastPull += extracted;
                    remaining -= extracted;
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
            lastCharge += accepted;
            setChanged();
        }
    }

    @Override
    public void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        buffer = Math.max(0, data.getLongOr(TAG_BUFFER, 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        data.putLong(TAG_BUFFER, buffer);
    }
}
