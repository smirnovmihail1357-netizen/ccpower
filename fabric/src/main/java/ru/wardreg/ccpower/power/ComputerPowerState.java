package ru.wardreg.ccpower.power;

import dan200.computercraft.core.computer.ComputerSide;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.wardreg.ccpower.power.CCPowerConfig;

/**
 * Per-computer power accounting state. One instance exists for every CC:Tweaked
 * computer id (computers and turtles alike - turtles are just computers with a
 * different energy supplier, per the mod design).
 *
 * <p>Thread-safety: tick accounting runs on the server thread, while surcharges and
 * the activity flag are set from the computer scheduler thread; those fields are
 * atomic/volatile. The turtle battery itself is synchronised in the mixin.
 */
public final class ComputerPowerState {
    public final int id;

    /**
     * Connected peripherals by side - the "network size" metric. The value is
     * the peripheral type string ({@code IPeripheral.getType()}, e.g.
     * {@code minecraft:chest}), used for the per-type costs.
     */
    private final ConcurrentHashMap<ComputerSide, String> peripherals = new ConcurrentHashMap<>();

    /** Surcharges (peripheral calls) accumulated since the last tick. */
    private final AtomicLong surcharge = new AtomicLong();

    /** Set by the scheduler thread when the Lua machine performed work. */
    private volatile boolean active;

    /** Turtle battery view, when this computer drives a turtle. */
    private final AtomicReference<TurtleEnergyView> turtle = new AtomicReference<>();

    /** Last known world position of this computer, refreshed every server tick. */
    private volatile Level level;

    private volatile BlockPos position;

    public ComputerPowerState(int id) {
        this.id = id;
    }

    /**
     * A peripheral appeared or disappeared on the given side.
     *
     * @param type the peripheral type string, or {@code null} when detached
     */
    public void peripheralChanged(ComputerSide side, String type) {
        if (type != null) {
            peripherals.put(side, type);
        } else {
            peripherals.remove(side);
        }
    }

    /** Connected peripheral types (live view, safe to iterate). */
    public Collection<String> peripheralTypes() {
        return peripherals.values();
    }

    public void addSurcharge(double rf) {
        long cap = (long) CCPowerConfig.surchargeCapPerTick();
        long add = (long) Math.ceil(rf);
        surcharge.accumulateAndGet(add, (current, delta) ->
            Math.min(cap, current + delta));
    }

    public long drainSurcharge() {
        return surcharge.getAndSet(0);
    }

    public void markActive() {
        active = true;
    }

    public boolean takeActive() {
        boolean was = active;
        active = false;
        return was;
    }

    public void registerTurtle(TurtleEnergyView view) {
        turtle.set(view);
    }

    public boolean hasTurtle() {
        return turtle.get() != null;
    }

    /** Try to pay the tick cost from the turtle battery. */
    public boolean consumeTurtle(long amount) {
        TurtleEnergyView view = turtle.get();
        return view != null && (amount <= 0 || view.ccpower$tryConsume(amount));
    }

    /** Remember the computer's world position so power-on checks can find FE neighbours. */
    public void updatePosition(Level newLevel, BlockPos newPos) {
        this.level = newLevel;
        this.position = newPos;
    }

    public Level level() {
        return level;
    }

    public BlockPos position() {
        return position;
    }

    /** RF currently stored in the turtle battery (0 for stationary computers). */
    public long turtleEnergy() {
        TurtleEnergyView view = turtle.get();
        return view != null ? view.ccpower$getEnergy() : 0;
    }

    /**
     * One-shot flag for the power-on denial log: CC:T retries the pending start
     * every tick, the message must only fire once per denial period.
     */
    private volatile boolean powerDeniedLogged;

    /** @return true when this is the first denial since energy was available */
    public boolean markPowerDeniedLogged() {
        if (powerDeniedLogged) return false;
        powerDeniedLogged = true;
        return true;
    }

    public void clearPowerDeniedLog() {
        powerDeniedLogged = false;
    }

    /**
     * Internal RF buffer of a stationary computer. Turtles use their own
     * battery instead (see {@link TurtleEnergyView}).
     */
    private final AtomicLong computerBuffer = new AtomicLong();

    /** RF currently stored in the computer's internal buffer. */
    public long bufferStored() {
        return computerBuffer.get();
    }

    /** Free space in the computer's internal buffer. */
    public long bufferRoom() {
        return Math.max(0, CCPowerConfig.computerBufferRF() - computerBuffer.get());
    }

    /** Push up to {@code amount} RF into the internal buffer. */
    public long receiveBuffer(long amount) {
        long room = bufferRoom();
        long accepted = Math.min(Math.max(0, amount), room);
        if (accepted > 0) computerBuffer.addAndGet(accepted);
        return accepted;
    }

    /**
     * Atomically consume {@code amount} RF from the internal buffer.
     *
     * @return true if the full amount was available and consumed
     */
    public boolean consumeBuffer(long amount) {
        if (amount <= 0) return true;
        long current = computerBuffer.get();
        while (current >= amount) {
            if (computerBuffer.compareAndSet(current, current - amount)) return true;
            current = computerBuffer.get();
        }
        return false;
    }

    /**
     * Reset per-tick accounting when the computer is off (or has just been
     * shut down due to a full discharge).
     */
    public void resetTick() {
        surcharge.set(0);
        active = false;
    }
}
