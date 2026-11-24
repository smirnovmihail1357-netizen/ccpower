package ru.wardreg.ccpower.tesla;

import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.util.NonNegativeId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.DeviceKind;
import ru.wardreg.ccpower.power.PowerManager;
import ru.wardreg.ccpower.registry.ModRegistry;

/**
 * Block entity of a Tesla Coil. It keeps its own RF buffer (refilled from
 * adjacent FE blocks), and every configured interval pulses that RF into
 * portable CC computers in range - pocket computers and Smart Glasses.
 */
public class TeslaCoilBlockEntity extends BlockEntity {
    private static final String TAG_BUFFER = "ccpower_rf";
    private static final String TAG_GIVEN = "ccpower_given";
    private static final String TAG_PULSES = "ccpower_pulses";

    private final boolean tall;
    private int ticks;
    private long buffer;
    private long totalGiven;
    private long pulses;
    private long lastPulse;
    private long startedAt = System.currentTimeMillis();

    private final TeslaPeripheral peripheral = new TeslaPeripheral(this);

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state, boolean tall) {
        super(tall ? ModRegistry.TESLA_BE_TALL.get() : ModRegistry.TESLA_BE_SMALL.get(), pos, state);
        this.tall = tall;
    }

    public TeslaPeripheral getPeripheral() {
        return peripheral;
    }

    public long stored() { return buffer; }
    public long capacity() { return CCPowerConfig.teslaBufferRF(); }
    public long totalGiven() { return totalGiven; }
    public long pulseCount() { return pulses; }
    public long lastPulseEnergy() { return lastPulse; }

    /** Average RF per second over the coil's lifetime. */
    public long deliveryRate() {
        long seconds = Math.max(1, (System.currentTimeMillis() - startedAt) / 1000);
        return totalGiven / seconds;
    }

    public long receive(long amount) {
        long accepted = Math.max(0, Math.min(amount, Math.max(0, capacity() - buffer)));
        if (accepted > 0) {
            buffer += accepted;
            setChanged();
        }
        return accepted;
    }

    public long extract(long amount) {
        long taken = Math.max(0, Math.min(amount, buffer));
        if (taken > 0) {
            buffer -= taken;
            setChanged();
        }
        return taken;
    }

    public static void serverTick(Level level, BlockPos pos, TeslaCoilBlockEntity coil) {
        if (!(level instanceof ServerLevel server)) return;
        refill(coil, server);
        if (++coil.ticks < CCPowerConfig.teslaIntervalSeconds() * 20) return;
        coil.ticks = 0;
        pulse(coil, server);
    }

    private static void refill(TeslaCoilBlockEntity coil, ServerLevel level) {
        long want = Math.min(coil.capacity() - coil.buffer, CCPowerConfig.computerPullRFPerTick());
        if (want <= 0) return;
        long got = 0;
        for (Direction dir : Direction.values()) {
            if (got >= want) break;
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                coil.getBlockPos().relative(dir), dir.getOpposite());
            if (storage == null || !storage.canExtract()) continue;
            got += storage.extractEnergy((int) Math.min(want - got, Integer.MAX_VALUE), false);
        }
        if (got > 0) coil.receive(got);
    }

    private static void pulse(TeslaCoilBlockEntity coil, ServerLevel level) {
        int radius = coil.tall
            ? CCPowerConfig.teslaLargeRadius()
            : CCPowerConfig.teslaSmallRadius();
        long limit = Math.min(CCPowerConfig.teslaEnergyPerTransfer(), coil.buffer);
        if (limit <= 0) return;
        AABB area = new AABB(coil.getBlockPos()).inflate(radius);

        long given = 0;
        for (Player player : level.getPlayers(
            p -> p.isAlive() && area.contains(p.getX(), p.getY(), p.getZ()))) {
            given += chargeInventory(player, limit - given);
            if (given >= limit) break;
        }
        if (given < limit) {
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
                given += chargeStack(entity.getItem(), limit - given);
                if (given >= limit) break;
            }
        }
        if (given > 0) {
            coil.buffer -= given;
            coil.totalGiven += given;
            coil.lastPulse = given;
            coil.pulses++;
            coil.setChanged();
        }
    }

    private static long chargeInventory(Player player, long budget) {
        if (budget <= 0) return 0;
        var inventory = player.getInventory();
        long given = 0;
        for (ItemStack stack : inventory.items) given += chargeStack(stack, budget - given);
        for (ItemStack stack : inventory.armor) given += chargeStack(stack, budget - given);
        given += chargeStack(inventory.offhand.get(0), budget - given);
        return given;
    }

    /** Charge only CC items that own one of our portable RF buffers. */
    static long chargeStack(ItemStack stack, long budget) {
        if (stack.isEmpty() || budget <= 0) return 0;

        DeviceKind kind = null;
        if (stack.getItem() instanceof PocketComputerItem) {
            kind = DeviceKind.POCKET;
        } else if (DeviceKind.isSmartGlassesItem(stack.getItem())) {
            kind = DeviceKind.SMART_GLASSES;
        }
        if (kind == null) return 0;

        NonNegativeId idValue = stack.get(
            dan200.computercraft.shared.ModRegistry.DataComponents.COMPUTER_ID.get());
        if (idValue == null) return 0;
        ComputerPowerState state = PowerManager.state(idValue.id());
        if (!state.kind().isPortable()) state.setKind(kind);
        return state.receivePortable(budget);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer = Math.max(0, tag.getLong(TAG_BUFFER));
        totalGiven = Math.max(0, tag.getLong(TAG_GIVEN));
        pulses = Math.max(0, tag.getLong(TAG_PULSES));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(TAG_BUFFER, buffer);
        tag.putLong(TAG_GIVEN, totalGiven);
        tag.putLong(TAG_PULSES, pulses);
    }
}
