package ru.wardreg.ccpower.power;

import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.core.ServerComputer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.CCPowerMod;
import team.reborn.energy.api.EnergyStorage;

/**
 * Central RF accounting for all CC:Tweaked computers (computers, turtles,
 * pocket computers - they all pass through the same code path here).
 *
 * <p>Fabric build: energy is read from adjacent blocks through the Team Reborn
 * Energy {@link EnergyStorage#SIDED} lookup. Everything else is identical to the
 * NeoForge build.
 */
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

    public static void onTickServer(ServerComputer computer) {
        int id = computer.getID();
        ComputerPowerState st = STATES.computeIfAbsent(id, ComputerPowerState::new);
        st.resetRates();

        st.updatePosition(computer.getLevel(), computer.getPosition());

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
        if (paid) st.recordDischarge(need);

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
        ServerLevel level = computer.getLevel();
        BlockPos pos = computer.getPosition();
        if (level == null || pos == null) return;

        long room = st.bufferRoom();
        if (room <= 0) return;
        long want = Math.min(room, CCPowerConfig.computerPullRFPerTick());
        long got = extractUpTo(level, pos, want);
        if (got > 0) {
            long accepted = st.receiveBuffer(got);
            st.recordCharge(accepted);
        }
    }

    /** Round-robin extraction from adjacent Team Reborn Energy storages. */
    private static long extractUpTo(ServerLevel level, BlockPos pos, long max) {
        long extracted = 0;
        for (Direction dir : Direction.values()) {
            if (extracted >= max) break;
            EnergyStorage storage =
                EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (storage == null || !storage.supportsExtraction()) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long got = storage.extract(max - extracted, tx);
                if (got > 0) {
                    tx.commit();
                    extracted += got;
                }
            }
        }
        return extracted;
    }
}
