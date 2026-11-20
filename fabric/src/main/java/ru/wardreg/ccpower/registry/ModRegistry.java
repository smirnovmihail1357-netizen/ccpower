package ru.wardreg.ccpower.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;
import team.reborn.energy.api.EnergyStorage;

/** Content registration (Fabric): dock block, item, block entity, tab, energy storage. */
public final class ModRegistry {
    public static final DockBlock DOCK = new DockBlock(DockBlock.blockProperties());
    public static final BlockItem DOCK_ITEM = new BlockItem(DOCK, new Item.Properties());
    public static final BlockEntityType<DockBlockEntity> DOCK_BE =
        BlockEntityType.Builder.of(DockBlockEntity::new, DOCK).build(null);

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, id("dock"), DOCK);
        Registry.register(BuiltInRegistries.ITEM, id("dock"), DOCK_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("dock"), DOCK_BE);
        EnergyStorage.SIDED.registerForBlockEntity((dock, side) -> dock.getStorage(), DOCK_BE);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CCPowerMod.MODID, path);
    }

    private ModRegistry() {}
}
