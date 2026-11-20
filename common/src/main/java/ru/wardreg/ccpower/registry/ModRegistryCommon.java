package ru.wardreg.ccpower.registry;

import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/** Holds the loader-created dock block-entity type for the shared dock code. */
public final class ModRegistryCommon {
    private static Supplier<? extends BlockEntityType<? extends DockBlockEntity>> dockBE;

    public static void init(Supplier<? extends BlockEntityType<? extends DockBlockEntity>> type) {
        dockBE = type;
    }

    public static BlockEntityType<? extends DockBlockEntity> dockBE() {
        if (dockBE == null) throw new IllegalStateException("Dock block entity not registered");
        return dockBE.get();
    }

    private ModRegistryCommon() {}
}
