package ru.wardreg.ccpower.power;

import dan200.computercraft.core.computer.ComputerSide;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.wardreg.ccpower.CCPowerConfig;

/** Per-computer power accounting state, shared by computers and turtles. */
public final class ComputerPowerState {
    public final int id;

    private final ConcurrentHashMap<ComputerSide, String> peripherals = new ConcurrentHashMap<>();
    private final AtomicLong surcharge = new AtomicLong();
    private volatile boolean active;
    private final AtomicReference<TurtleEnergyView> turtle = new AtomicReference<>();

    private volatile World level;
    private volatile BlockPos position;

    public ComputerPowerState(int id) {
        this.id = id;
    }

    public void peripheralChanged(ComputerSide side, String type) {
        if (type != null) peripherals.put(side, type);
        else peripherals.remove(side);
    }

    public Collection<String> peripheralTypes() {
        return peripherals.values();
    }

    public void addSurcharge(double rf) {
        long cap = (long) CCPowerConfig.surchargeCapPerTick();
        long add = (long) Math.ceil(rf);
        surcharge.accumulateAndGet(add, (current, delta) -> Math.min(cap, current + delta));
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

    public boolean consumeTurtle(long amount) {
        TurtleEnergyView view = turtle.get();
        return view != null && (amount <= 0 || view.ccpower$tryConsume(amount));
    }

    public void updatePosition(World newLevel, BlockPos newPos) {
        this.level = newLevel;
        this.position = newPos;
    }

    public World level() {
        return level;
    }

    public BlockPos position() {
        return position;
    }

    public long turtleEnergy() {
        TurtleEnergyView view = turtle.get();
        return view != null ? view.ccpower$getEnergy() : 0;
    }

    /** RF received during the last server tick (FE neighbours or the dock). */
    private final AtomicLong lastCharge = new AtomicLong();

    /** RF spent during the last server tick (tick cost + turtle actions). */
    private final AtomicLong lastDischarge = new AtomicLong();

    /** Record RF pushed into this computer's buffer/battery this tick. */
    public void recordCharge(long amount) {
        if (amount > 0) lastCharge.addAndGet(amount);
    }

    /** Record RF taken out of this computer's buffer/battery this tick. */
    public void recordDischarge(long amount) {
        if (amount > 0) lastDischarge.addAndGet(amount);
    }

    /** RF/tick received on the last completed server tick. */
    public long chargeRate() {
        return lastCharge.get();
    }

    /** RF/tick spent on the last completed server tick. */
    public long dischargeRate() {
        return lastDischarge.get();
    }

    /** Clear the per-tick rate counters before a new accounting tick. */
    public void resetRates() {
        lastCharge.set(0);
        lastDischarge.set(0);
    }

    /** Energy currently held by this computer (buffer for PCs, battery for turtles). */
    public long energyStored() {
        return hasTurtle() ? turtleEnergy() : bufferStored();
    }

    /** Capacity of the energy store used by this computer. */
    public long energyCapacity() {
        return hasTurtle()
            ? CCPowerConfig.turtleBufferRF()
            : CCPowerConfig.computerBufferRF();
    }

    private volatile boolean powerDeniedLogged;

    public boolean markPowerDeniedLogged() {
        if (powerDeniedLogged) return false;
        powerDeniedLogged = true;
        return true;
    }

    public void clearPowerDeniedLog() {
        powerDeniedLogged = false;
    }

    private final AtomicLong computerBuffer = new AtomicLong();

    public long bufferStored() {
        return computerBuffer.get();
    }

    public long bufferRoom() {
        return Math.max(0, CCPowerConfig.computerBufferRF() - computerBuffer.get());
    }

    public long receiveBuffer(long amount) {
        long room = bufferRoom();
        long accepted = Math.min(Math.max(0, amount), room);
        if (accepted > 0) computerBuffer.addAndGet(accepted);
        return accepted;
    }

    public boolean consumeBuffer(long amount) {
        if (amount <= 0) return true;
        long current = computerBuffer.get();
        while (current >= amount) {
            if (computerBuffer.compareAndSet(current, current - amount)) return true;
            current = computerBuffer.get();
        }
        return false;
    }

    public void resetTick() {
        surcharge.set(0);
        active = false;
    }
}
