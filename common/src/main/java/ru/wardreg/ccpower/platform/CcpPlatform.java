package ru.wardreg.ccpower.platform;

import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/**
 * Everything the shared code needs from the mod loader. Implementations live
 * in the fabric/ and forge/ modules; the common module never imports a loader
 * class directly.
 */
public interface CcpPlatform {
    /** Directory holding ccpower-*.toml. */
    Path configDir();

    /** Human-friendly display name of a mod namespace. */
    String modDisplayName(String modid);

    /**
     * Ask a freshly created block entity for a CC:T peripheral through the
     * loader's native lookup. {@code null} means "not probeable without a
     * live level"; runtime attach-time discovery still covers those types.
     */
    String probePeripheral(Level level, BlockState state, BlockEntity be);

    /** Extract up to {@code max} RF from the FE storage at one position. */
    long extractEnergy(Level level, BlockPos pos, Direction side, long max);

    /** Register the dock as a CC:T peripheral through the loader's native API. */
    void registerDockPeripheral();

    /** Create the loader-neutral energy view of a dock's buffer. */
    EnergyView createDockEnergy(DockBlockEntity dock);

    /** Create the dock BE (Forge needs a subclass exposing the FE capability). */
    DockBlockEntity createDockBlockEntity(net.minecraft.core.BlockPos pos, BlockState state);
}
