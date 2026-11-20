package ru.wardreg.ccpower.fabric;

import java.nio.file.Path;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.wardreg.ccpower.dock.DockBlockEntity;
import ru.wardreg.ccpower.platform.CcpPlatform;
import ru.wardreg.ccpower.platform.EnergyView;
import team.reborn.energy.api.EnergyStorage;

/** Fabric implementation of the platform hooks (Team Reborn Energy 3.x). */
public final class FabricPlatform implements CcpPlatform {
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public String modDisplayName(String modid) {
        return FabricLoader.getInstance().getModContainer(modid)
            .map(c -> c.getMetadata().getName())
            .filter(name -> name != null && !name.isBlank())
            .orElse(modid);
    }

    /**
     * CC:T's Fabric peripheral lookup is a BlockApiLookup bound to real world
     * positions, so an unplaced block entity cannot be probed here. Types whose
     * peripheral comes from a capability/lookup are discovered at attach time
     * by the PeripheralAPI mixin instead.
     */
    public String probePeripheral(Level level, BlockState state, BlockEntity be) {
        return null;
    }

    public long extractEnergy(Level level, BlockPos pos, Direction side, long max) {
        try {
            EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, side);
            if (storage == null || !storage.supportsExtraction()) return 0;
            try (Transaction tx = Transaction.openOuter()) {
                long got = storage.extract(max, tx);
                tx.commit();
                return got;
            }
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public DockBlockEntity createDockBlockEntity(BlockPos pos, BlockState state) {
        return new DockBlockEntity(pos, state);
    }

    public EnergyView createDockEnergy(DockBlockEntity dock) {
        return new EnergyView() {
            public long receive(long amount, boolean simulate) { return dock.receive(amount, simulate); }
            public long getAmount() { return dock.getBuffer(); }
            public long getCapacity() { return dock.getBufferCapacity(); }
        };
    }
}
