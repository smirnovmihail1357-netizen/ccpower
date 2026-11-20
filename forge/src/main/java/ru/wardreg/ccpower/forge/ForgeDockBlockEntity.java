package ru.wardreg.ccpower.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/** Forge 47 exposes block-entity capabilities by overriding getCapability. */
public final class ForgeDockBlockEntity extends DockBlockEntity {
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new IEnergyStorage() {
        public int receiveEnergy(int toReceive, boolean simulate) {
            return (int) getStorage().receive(toReceive, simulate);
        }
        public int extractEnergy(int toExtract, boolean simulate) { return 0; }
        public int getEnergyStored() { return (int) Math.min(getBuffer(), Integer.MAX_VALUE); }
        public int getMaxEnergyStored() { return (int) Math.min(getBufferCapacity(), Integer.MAX_VALUE); }
        public boolean canExtract() { return false; }
        public boolean canReceive() { return true; }
    });

    public ForgeDockBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }
}
