package ru.wardreg.ccpower.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;
import team.reborn.energy.api.EnergyStorage;

/** Fabric content registration: dock block, item, block entity, tab, energy provider. */
public final class ModRegistry {
    private static final ResourceLocation DOCK_ID = id("dock");

    public static final DockBlock DOCK = new DockBlock(DockBlock.blockProperties()
        .setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK, DOCK_ID)));

    public static final BlockItem DOCK_ITEM = new BlockItem(DOCK,
        new net.minecraft.world.item.Item.Properties()
            .setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, DOCK_ID)));

    public static final BlockEntityType<DockBlockEntity> DOCK_BE =
        FabricBlockEntityTypeBuilder.create(DockBlockEntity::new, DOCK).build();

    public static final CreativeModeTab MAIN_TAB = FabricItemGroup.builder()
        .title(Component.translatable("itemGroup.ccpower"))
        .icon(() -> new ItemStack(DOCK_ITEM))
        .displayItems((params, output) -> output.accept(DOCK_ITEM))
        .build();

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, DOCK_ID, DOCK);
        Registry.register(BuiltInRegistries.ITEM, id("dock"), DOCK_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("dock"), DOCK_BE);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), MAIN_TAB);

        // Expose the dock's internal buffer as a Team Reborn Energy storage so
        // Fabric cables can push energy into it.
        EnergyStorage.SIDED.registerForBlockEntity(
            (dock, side) -> dock.getStorage(), DOCK_BE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CCPowerMod.MODID, path);
    }

    private ModRegistry() {}
}
