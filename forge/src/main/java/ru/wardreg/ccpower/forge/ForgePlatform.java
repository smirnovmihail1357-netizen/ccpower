package ru.wardreg.ccpower.forge;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.Capabilities;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import ru.wardreg.ccpower.dock.DockBlockEntity;
import ru.wardreg.ccpower.platform.CcpPlatform;
import ru.wardreg.ccpower.platform.EnergyView;

/** Forge implementation of the platform hooks. */
public final class ForgePlatform implements CcpPlatform {
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public String modDisplayName(String modid) {
        return ModList.get().getModContainerById(modid)
            .map(c -> c.getModInfo().getDisplayName())
            .filter(name -> name != null && !name.isBlank())
            .orElse(modid);
    }

    public String probePeripheral(Level level, BlockState state, BlockEntity be) {
        try {
            for (Direction side : Direction.values()) {
                LazyOptional<IPeripheral> cap = be.getCapability(Capabilities.CAPABILITY_PERIPHERAL, side);
                String type = cap.resolve().map(IPeripheral::getType).orElse(null);
                if (type != null && !type.isBlank()) return type;
            }
        } catch (Throwable ignored) {
            // A provider needing a live level is not pre-scanable.
        }
        return null;
    }

    public long extractEnergy(Level level, BlockPos pos, Direction side, long max) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return 0;
            LazyOptional<IEnergyStorage> lazy = be.getCapability(ForgeCapabilities.ENERGY, side);
            IEnergyStorage storage = lazy.resolve().orElse(null);
            if (storage == null || !storage.canExtract()) return 0;
            return storage.extractEnergy((int) Math.min(max, Integer.MAX_VALUE), false);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public DockBlockEntity createDockBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeDockBlockEntity(pos, state);
    }

    public void registerDockPeripheral() {
        ForgeComputerCraftAPI.registerPeripheralProvider((level, pos, side) -> {
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof DockBlockEntity dock
                ? LazyOptional.of(() -> dock.getPeripheral())
                : LazyOptional.empty();
        });
    }

    public EnergyView createDockEnergy(DockBlockEntity dock) {
        return new EnergyView() {
            public long receive(long amount, boolean simulate) { return dock.receive(amount, simulate); }
            public long getAmount() { return dock.getBuffer(); }
            public long getCapacity() { return dock.getBufferCapacity(); }
        };
    }
}
