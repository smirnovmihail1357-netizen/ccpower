package ru.wardreg.ccpower.dock;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.platform.CcpPlatformHolder;
import ru.wardreg.ccpower.platform.EnergyView;
import ru.wardreg.ccpower.power.TurtleEnergyView;
import ru.wardreg.ccpower.registry.ModRegistryCommon;

/**
 * Block entity of the Recharge Dock. Every server tick it:
 * <ol>
 *   <li>pulls up to {@code dock.pullRFPerTick} RF from adjacent FE storages into its
 *       own buffer (the buffer is also exposed through the loader's energy API,
 *       so pipes can push);</li>
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

    /** Native energy view of the buffer, registered by the loader at startup. */
    public final EnergyView energy = CcpPlatformHolder.get().createDockEnergy(this);

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistryCommon.dockBE(), pos, state);
    }

    /** @return the loader-neutral energy view (wrapped by the loader's API). */
    public EnergyView getStorage() {
        return energy;
    }

    /** Insert RF into the dock buffer, optionally simulating the transfer. */
    public long receive(long amount, boolean simulate) {
        long space = CCPowerConfig.dockBufferRF() - buffer;
        long accepted = Math.max(0, Math.min(amount, space));
        if (!simulate && accepted > 0) {
            buffer += accepted;
            setChanged();
        }
        return accepted;
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
            long extracted = CcpPlatformHolder.get().extractEnergy(
                level, pos.relative(dir), dir.getOpposite(), remaining);
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
