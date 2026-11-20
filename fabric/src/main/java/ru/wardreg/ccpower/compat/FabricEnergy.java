package ru.wardreg.ccpower.compat;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;

/** Team Reborn Energy lookups and transaction wrappers (Fabric FE/RF API). */
public final class FabricEnergy {
    private FabricEnergy() {}

    /** Extract up to {@code max} RF from the FE storage exposed on the given side. */
    public static long extract(Level level, BlockPos pos, Direction side, long max) {
        if (max <= 0) return 0;
        EnergyStorage storage = find(level, pos, side);
        if (storage == null || !storage.supportsExtraction()) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long got = storage.extract(max, tx);
            tx.commit();
            return Math.max(0L, got);
        }
    }

    private static EnergyStorage find(Level level, BlockPos pos, Direction side) {
        return EnergyStorage.SIDED.find(level, pos, side);
    }
}
