package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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

    /** RF pulled from neighbours on the last server tick. */
    private long lastPull;

    /** RF transferred into a turtle on the last server tick. */
    private long lastCharge;

    /** Cached CC:T peripheral exposing the dock's state to Lua. */
    private final DockPeripheral peripheral = new DockPeripheral(this);

    /**
     * FE view of the dock's buffer, registered as a block capability.
     *
     * <p>MC 26.x replaced {@code IEnergyStorage} with {@code EnergyHandler} and
     * {@code Capabilities.EnergyStorage.BLOCK} with {@code Capabilities.Energy.BLOCK}.
     * The dock still accepts energy but never emits it through the capability:
     * it only feeds the turtle standing on top.</p>
     */
    private final EnergyHandler storage = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return buffer;
        }

        @Override
        public long getCapacityAsLong() {
            return CCPowerConfig.dockBufferRF();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            long space = CCPowerConfig.dockBufferRF() - buffer;
            int accepted = (int) Math.max(0, Math.min(amount, space));
            if (accepted > 0) {
                buffer += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    };

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DOCK_BE.get(), pos, state);
    }

    public EnergyHandler getStorage() {
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

    /** Server-side tick: pull RF from neighbours, charge the turtle above. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DockBlockEntity dock) {
        dock.lastPull = 0;
        dock.lastCharge = 0;
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
            EnergyHandler neighbour = level.getCapability(
                Capabilities.Energy.BLOCK, pos.relative(dir), dir);
            if (neighbour == null) continue;
            int extracted;
            try (Transaction tx = Transaction.openRoot()) {
                extracted = neighbour.extract((int) Math.min(remaining, Integer.MAX_VALUE), tx);
                if (extracted > 0) tx.commit();
            }
            if (extracted > 0) {
                buffer += extracted;
                lastPull += extracted;
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer = Math.max(0, input.getLongOr(TAG_BUFFER, 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_BUFFER, buffer);
    }
}
