package ru.wardreg.ccpower.power;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import ru.wardreg.ccpower.CCPowerConfig;

/**
 * RF accounting for peripheral blocks from Advanced Peripherals.
 *
 * <p>The mod is optional: block entities are recognised by their registry
 * namespace, so no class from Advanced Peripherals is ever referenced here.
 * A block pays one flat cost while it is attached to at least one computer
 * network - multiple attachments do not multiply the cost, and method calls
 * are not billed here (all of that happens in the computer).
 */
public final class APPeripheralManager {
    private static final String AP_NAMESPACE = "advancedperipherals";

    private record DeviceKey(ResourceLocation level, BlockPos pos) {}

    private static final Map<DeviceKey, APPeripheralState> STATES = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<ComputerSide, DeviceKey>> LINKS = new ConcurrentHashMap<>();

    private APPeripheralManager() {}

    /** Called from the PeripheralAPI mixin whenever a peripheral attaches/detaches. */
    public static void onPeripheralChanged(int computerId, ComputerSide side, IPeripheral peripheral) {
        if (peripheral != null) {
            Object target = peripheral.getTarget();
            if (!(target instanceof BlockEntity blockEntity) || !isAdvancedPeripherals(blockEntity)) return;
            if (!(blockEntity.getLevel() instanceof ServerLevel level)) return;

            DeviceKey key = new DeviceKey(level.dimension().location(), blockEntity.getBlockPos());
            STATES.computeIfAbsent(key, k -> new APPeripheralState(level, blockEntity.getBlockPos()))
                .addConnection();
            LINKS.computeIfAbsent(computerId, ignored -> new ConcurrentHashMap<>()).put(side, key);
        } else {
            Map<ComputerSide, DeviceKey> sides = LINKS.get(computerId);
            if (sides == null) return;
            DeviceKey key = sides.remove(side);
            if (key == null) return;
            APPeripheralState state = STATES.get(key);
            if (state != null) state.removeConnection();
            if (sides.isEmpty()) LINKS.remove(computerId, sides);
        }
    }

    /** @return the RF state of an AP peripheral block, or {@code null}. */
    public static APPeripheralState stateAt(BlockEntity blockEntity) {
        if (!isAdvancedPeripherals(blockEntity) || blockEntity.getLevel() == null) return null;
        DeviceKey key = new DeviceKey(
            blockEntity.getLevel().dimension().location(), blockEntity.getBlockPos());
        return STATES.get(key);
    }

    /** @return true when the block exists, is connected and cannot pay its cost. */
    public static boolean isOffline(BlockEntity blockEntity) {
        APPeripheralState state = stateAt(blockEntity);
        return state != null && state.isConnected() && !state.isOnline();
    }

    /** Server tick: refill buffers and bill connected blocks exactly once. */
    public static void tick() {
        long cost = (long) Math.ceil(CCPowerConfig.advancedPeripheralsPeripheralRFPerTick());
        for (Map.Entry<DeviceKey, APPeripheralState> entry : STATES.entrySet()) {
            APPeripheralState state = entry.getValue();
            refill(state);
            if (!state.isConnected()) {
                state.setOnline(true);
                if (state.stored() <= 0) STATES.remove(entry.getKey(), state);
                continue;
            }
            boolean paid = state.consume(cost);
            state.setOnline(paid);
        }
    }

    private static void refill(APPeripheralState state) {
        ServerLevel level = state.level();
        BlockPos pos = state.pos();
        long room = state.room();
        if (room <= 0) return;
        long want = Math.min(room, CCPowerConfig.computerPullRFPerTick());
        long got = 0;
        for (Direction dir : Direction.values()) {
            if (got >= want) break;
            IEnergyStorage storage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (storage == null || !storage.canExtract()) continue;
            got += storage.extractEnergy((int) Math.min(want - got, Integer.MAX_VALUE), false);
        }
        if (got > 0) state.receive(got);
    }

    private static boolean isAdvancedPeripherals(BlockEntity blockEntity) {
        if (!DeviceKind.advancedPeripheralsLoaded() || blockEntity == null) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        return id != null && AP_NAMESPACE.equals(id.getNamespace());
    }

    /** For Jade: all states that have a live block entity (snapshot). */
    public static Collection<APPeripheralState> states() {
        return STATES.values();
    }
}
