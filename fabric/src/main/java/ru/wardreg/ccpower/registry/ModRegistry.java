package ru.wardreg.ccpower.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/** Fabric content registration and the dock's FE capability. */
public final class ModRegistry {
    public static final ResourceLocation DOCK_ID = id("dock");

    public static final DockBlock DOCK = Registry.register(BuiltInRegistries.BLOCK, DOCK_ID,
        new DockBlock(DockBlock.blockProperties()));
    public static final BlockItem DOCK_ITEM = Registry.register(BuiltInRegistries.ITEM, DOCK_ID,
        new BlockItem(DOCK, new Item.Properties()));
    public static final BlockEntityType<DockBlockEntity> DOCK_BE =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DOCK_ID,
            BlockEntityType.Builder.of(DockBlockEntity::new, DOCK).build(null));
    public static final CreativeModeTab MAIN_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"),
        FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.ccpower"))
            .icon(() -> new ItemStack(DOCK_ITEM))
            .displayItems((params, output) -> output.accept(DOCK_ITEM))
            .build());

    public static void register() {
        EnergyStorage.SIDED.registerForBlockEntity(
            (dock, side) -> dock.getStorage(), DOCK_BE);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("ccpower", path);
    }

    private ModRegistry() {}
}
