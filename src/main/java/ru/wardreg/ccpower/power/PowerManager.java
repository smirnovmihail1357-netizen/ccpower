package ru.wardreg.ccpower.power;

import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.core.ServerComputer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.CCPowerMod;

/** Central RF accounting for all CC:Tweaked computers and turtles. */
public final class PowerManager {
    private static final Map<Integer, ComputerPowerState> STATES = new ConcurrentHashMap<>();

    private PowerManager() {}

    public static ComputerPowerState state(int id) {
        return STATES.computeIfAbsent(id, ComputerPowerState::new);
    }

    public static ComputerPowerState stateOrNull(int id) {
        return STATES.get(id);
    }

    public static void chargePeripheralCall(int id, String type) {
        state(id).addSurcharge(PeripheralCosts.callCost(type));
    }

    public static void onPeripheralChanged(int id, ComputerSide side, String type) {
        state(id).peripheralChanged(side, type);
    }

    public static void registerTurtle(int id, TurtleEnergyView turtle) {
        state(id).registerTurtle(turtle);
    }

    public static void markActive(int id) {
        ComputerPowerState st = STATES.get(id);
        if (st != null) st.markActive();
    }

    /** Called at the head of {@code ServerComputer.update()}. */
    public static void onTickServer(ServerComputer computer) {
        int id = computer.getID();
        ComputerPowerState st = STATES.computeIfAbsent(id, ComputerPowerState::new);

        st.updatePosition(computer.getWorld(), computer.getPosition());
        boolean stationary = !st.hasTurtle();

        if (!computer.isOn()) {
            st.resetTick();
            if (stationary) refillBuffer(computer, st);
            return;
        }

        if (stationary) refillBuffer(computer, st);

        boolean assumeActive = st.takeActive();
        double peripheralCost = 0.0;
        for (String type : st.peripheralTypes()) {
            peripheralCost += PeripheralCosts.tickCost(type);
        }
        double cost = CCPowerConfig.idleRFPerTick()
            + (assumeActive ? CCPowerConfig.activeRFPerTick() : 0.0)
            + peripheralCost
            + st.drainSurcharge();
        long need = (long) Math.ceil(cost);

        boolean paid = stationary ? st.consumeBuffer(need) : st.consumeTurtle(need);

        if (!paid) {
            logDischarged(id, need, stationary ? st.bufferStored() : st.turtleEnergy());
            computer.shutdown();
            st.resetTick();
        }
    }

    public static boolean canPowerOn(int id) {
        ComputerPowerState st = state(id);
        long need = (long) Math.ceil(
            CCPowerConfig.idleRFPerTick() + CCPowerConfig.activeRFPerTick());
        if (st.hasTurtle()) {
            boolean allowed = st.turtleEnergy() >= need;
            if (allowed) st.clearPowerDeniedLog();
            return allowed;
        }
        // Pocket computers have no position and no way to recharge: grace.
        if (st.level() == null || st.position() == null) return true;
        boolean allowed = st.bufferStored() >= need;
        if (allowed) st.clearPowerDeniedLog();
        return allowed;
    }

    public static void logPowerOnDenied(int id) {
        if (!CCPowerConfig.loggingEnabled() || !CCPowerConfig.logStateTransitions()) return;
        ComputerPowerState st = stateOrNull(id);
        if (st != null && st.markPowerDeniedLogged()) {
            CCPowerMod.LOG.info("Power-on of computer #{} denied: no RF energy available", id);
        }
    }

    public static void logDischarged(int id, long need, long available) {
        if (CCPowerConfig.loggingEnabled() && CCPowerConfig.logStateTransitions()) {
            CCPowerMod.LOG.info(
                "Computer #{} fully discharged (needs {} RF/tick, buffer holds {} RF), shutting down",
                id, need, available);
        }
    }

    private static void refillBuffer(ServerComputer computer, ComputerPowerState st) {
        World level = computer.getWorld();
        BlockPos pos = computer.getPosition();
        if (level == null || pos == null) return;

        long room = st.bufferRoom();
        if (room <= 0) return;
        long want = Math.min(room, CCPowerConfig.computerPullRFPerTick());
        long got = extractUpTo(level, pos, want);
        if (got > 0) st.receiveBuffer(got);
    }

    private static long extractUpTo(World level, BlockPos pos, long max) {
        long extracted = 0;
        for (Direction dir : Direction.values()) {
            if (extracted >= max) break;
            TileEntity neighbour = level.getBlockEntity(pos.relative(dir));
            if (neighbour == null) continue;
            IEnergyStorage storage = neighbour
                .getCapability(CapabilityEnergy.ENERGY, dir.getOpposite()).orElse(null);
            if (storage == null || !storage.canExtract()) continue;
            extracted += storage.extractEnergy(
                (int) Math.min(max - extracted, Integer.MAX_VALUE), false);
        }
        return extracted;
    }
}
