package ru.wardreg.ccpower.power;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Per-block RF state of an Advanced Peripherals peripheral block. */
public final class APPeripheralState {
    private final ServerLevel level;
    private final BlockPos pos;
    private final AtomicLong buffer = new AtomicLong();
    private final AtomicInteger connections = new AtomicInteger();
    private volatile boolean online = true;

    public APPeripheralState(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    public ServerLevel level() { return level; }
    public BlockPos pos() { return pos; }

    public void addConnection() { connections.incrementAndGet(); }

    public void removeConnection() {
        connections.updateAndGet(n -> Math.max(0, n - 1));
    }

    /** True when at least one computer has this block attached. */
    public boolean isConnected() { return connections.get() > 0; }

    public long stored() { return buffer.get(); }

    public long room() {
        return Math.max(0, ru.wardreg.ccpower.CCPowerConfig.advancedPeripheralsBufferRF() - buffer.get());
    }

    public long receive(long amount) {
        long accepted = Math.min(Math.max(0, amount), room());
        if (accepted > 0) buffer.addAndGet(accepted);
        return accepted;
    }

    public boolean consume(long amount) {
        if (amount <= 0) return true;
        long current = buffer.get();
        while (current >= amount) {
            if (buffer.compareAndSet(current, current - amount)) return true;
            current = buffer.get();
        }
        return false;
    }

    public boolean isOnline() { return online; }
    public void setOnline(boolean value) { online = value; }
}
