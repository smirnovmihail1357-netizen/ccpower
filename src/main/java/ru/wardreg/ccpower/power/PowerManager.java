package ru.wardreg.ccpower.power;

import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.core.ServerComputer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.CCPowerMod;

/**
 * Central RF accounting for all CC:Tweaked computers (computers, turtles,
 * pocket computers - they all pass through the same code path here).
 *
 * <p>Every computer carries a small internal RF buffer (configurable in
 * {@code ccpower-common.toml}), from which the tick cost is drawn. Buffers
 * are refilled from the outside:
 * <ul>
 *   <li>a stationary computer continuously pulls RF from adjacent FE blocks
 *       at {@code power.computerPullRFPerTick};</li>
 *   <li>a turtle is charged only at the Recharge Dock.</li>
 * </ul>
 * A turned-off computer consumes nothing at all (it still recharges). A
 * running computer that cannot pay its tick cost is fully discharged: it is
 * shut down like a normal CC shutdown - the running program is lost and there
 * is no automatic resume. Power-on is denied while the buffer cannot cover a
 * single running tick.
 */
public final class PowerManager {
    /** Per-computer-id accounting states. */
    private static final Map<Integer, ComputerPowerState> STATES = new ConcurrentHashMap<>();

    private PowerManager() {}

    public static ComputerPowerState state(int id) {
        return STATES.computeIfAbsent(id, ComputerPowerState::new);
    }

    public static ComputerPowerState stateOrNull(int id) {
        return STATES.get(id);
    }

    /** Called from the PeripheralWrapper mixin on every peripheral method call. */
    public static void chargePeripheralCall(int id, String type) {
        ComputerPowerState st = state(id);
        double cost = st.kind().isPortable()
            ? PortableCosts.callCost(st.kind(), type)
            : PeripheralCosts.callCost(type);
        st.addSurcharge(cost);
    }

    /**
     * Called from the PeripheralAPI mixin when a peripheral appears or disappears.
     * The type string (e.g. {@code minecraft:chest}) feeds the per-type cost
     * table in {@code ccpower-peripherals.toml}.
     */
    public static void onPeripheralChanged(int id, ComputerSide side, String type) {
        state(id).peripheralChanged(side, type);
    }

    /** Called from the TurtleBrain mixin when the brain is bound to a computer. */
    public static void registerTurtle(int id, TurtleEnergyView turtle) {
        state(id).registerTurtle(turtle);
    }

    /** Called from the ComputerExecutor mixin whenever Lua work was executed. */
    public static void markActive(int id) {
        ComputerPowerState st = STATES.get(id);
        if (st != null) st.markActive();
    }

    /**
     * Main entry point, called at the head of {@code ServerComputer.tickServer()}.
     * Recharges the internal buffer and draws the tick cost from it. A computer
     * that cannot pay (i.e. is fully discharged) is shut down; power-on is
     * gated in the ComputerExecutor mixin.
     */
    public static void onTickServer(ServerComputer computer) {
        int id = computer.getID();
        ComputerPowerState st = STATES.computeIfAbsent(id, ComputerPowerState::new);
        st.resetRates();

        // Track the position even while off, so power-on checks and the FE
        // recharge can locate the computer's neighbours.
        st.updatePosition(computer.getLevel(), computer.getPosition());

        DeviceKind kind = DeviceKind.of(computer);
        st.setKind(kind);
        boolean turtle = st.hasTurtle();
        boolean portable = !turtle && kind.isPortable();
        boolean stationary = !turtle && !portable;

        if (!computer.isOn()) {
            // A turned-off computer consumes nothing, but a stationary one still recharges.
            st.resetTick();
            if (stationary) refillBuffer(computer, st);
            return;
        }

        // Stationary computers keep pulling RF from their neighbours while on.
        if (stationary) refillBuffer(computer, st);

        boolean assumeActive = st.takeActive();
        double surcharge = st.drainSurcharge();
        double cost;
        if (portable) {
            cost = PortableCosts.tickCost(kind, st, assumeActive, surcharge);
        } else {
            double peripheralCost = 0.0;
            for (String type : st.peripheralTypes()) {
                peripheralCost += PeripheralCosts.tickCost(type);
            }
            cost = CCPowerConfig.idleRFPerTick()
                + (assumeActive ? CCPowerConfig.activeRFPerTick() : 0.0)
                + peripheralCost
                + surcharge;
        }
        long need = (long) Math.ceil(cost);

        boolean paid;
        if (turtle) paid = st.consumeTurtle(need);
        else if (portable) paid = st.consumePortable(need);
        else paid = st.consumeBuffer(need);
        if (paid) st.recordDischarge(need);
        if (portable) st.syncPortableToStack();

        if (!paid) {
            // Fully discharged: shut the computer down. The running program is
            // lost exactly as with a normal CC shutdown - no automatic resume,
            // turning the machine back on is up to the player.
            long available = turtle ? st.turtleEnergy()
                : portable ? st.portableStored()
                : st.bufferStored();
            logDischarged(id, need, available);
            computer.shutdown();
            st.resetTick();
        }
    }

    /**
     * Whether a computer may be turned on right now: its internal buffer must
     * hold at least one full running tick (idle + active). Pocket computers
     * without a resolved position are always allowed.
     *
     * <p>This is checked when CC:T queues a power-on, so a computer without
     * energy never boots at all - it simply stays off.
     */
    public static boolean canPowerOn(int id) {
        ComputerPowerState st = state(id);
        long need = (long) Math.ceil(
            CCPowerConfig.idleRFPerTick() + CCPowerConfig.activeRFPerTick());
        if (st.hasTurtle()) {
            boolean allowed = st.turtleEnergy() >= need;
            if (allowed) st.clearPowerDeniedLog();
            return allowed;
        }
        if (st.kind().isPortable()) {
            long portableNeed = (long) Math.ceil(
                PortableCosts.tickCost(st.kind(), st, true, 0.0));
            boolean allowed = st.portableStored() >= portableNeed;
            if (allowed) st.clearPowerDeniedLog();
            return allowed;
        }
        // Devices without a resolved position and no way to recharge: grace.
        if (st.level() == null || st.position() == null) return true;
        boolean allowed = st.bufferStored() >= need;
        if (allowed) st.clearPowerDeniedLog();
        return allowed;
    }

    /**
     * Log a denied power-on attempt. CC:T retries the pending start request
     * every tick, so the message is logged once per computer and re-armed only
     * after energy becomes available again.
     */
    public static void logPowerOnDenied(int id) {
        if (!CCPowerConfig.loggingEnabled() || !CCPowerConfig.logStateTransitions()) return;
        ComputerPowerState st = stateOrNull(id);
        if (st != null && st.markPowerDeniedLogged()) {
            CCPowerMod.LOG.info("Power-on of computer #{} denied: no RF energy available", id);
        }
    }

    /** Diagnostic: why a computer just shut down. */
    public static void logDischarged(int id, long need, long available) {
        if (CCPowerConfig.loggingEnabled() && CCPowerConfig.logStateTransitions()) {
            CCPowerMod.LOG.info(
                "Computer #{} fully discharged (needs {} RF/tick, buffer holds {} RF), shutting down",
                id, need, available);
        }
    }

    /**
     * Pull RF from the FE storages adjacent to the computer into its internal
     * buffer, up to {@code power.computerPullRFPerTick} per tick.
     */
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

    /**
     * Extract up to {@code max} RF from FE storages adjacent to the given
     * position, round-robin, without any availability pre-check.
     *
     * @return the amount actually extracted
     */
    private static long extractUpTo(ServerLevel level, BlockPos pos, long max) {
        long extracted = 0;
        for (Direction dir : Direction.values()) {
            if (extracted >= max) break;
            IEnergyStorage storage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (storage == null || !storage.canExtract()) continue;
            extracted += storage.extractEnergy(
                (int) Math.min(max - extracted, Integer.MAX_VALUE), false);
        }
        return extracted;
    }
}
