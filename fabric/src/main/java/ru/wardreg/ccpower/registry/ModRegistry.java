package ru.wardreg.ccpower.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/**
 * Mod content registration (Fabric flavour): the Recharge Dock block, its item,
 * block entity, creative tab and the Team Reborn Energy block storage.
 */
public final class ModRegistry {
    private static final Identifier DOCK_ID =
        Identifier.fromNamespaceAndPath(CCPowerMod.MODID, "dock");

    public static BlockEntityType<DockBlockEntity> DOCK_BE;

    public static void register() {
        DockBlock dock = Registry.register(BuiltInRegistries.BLOCK, DOCK_ID,
            new DockBlock(DockBlock.blockProperties()));
        Registry.register(BuiltInRegistries.ITEM, DOCK_ID,
            new BlockItem(dock, new net.minecraft.world.item.Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, DOCK_ID))));

        DOCK_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DOCK_ID,
            FabricBlockEntityTypeBuilder.create(DockBlockEntity::new, dock).build());

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(CCPowerMod.MODID, "main"),
            FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.ccpower"))
                .icon(() -> new ItemStack(dock))
                .displayItems((params, output) -> output.accept(dock))
                .build());

        // External cables can push FE into the dock through this storage.
        EnergyStorage.SIDED.registerForBlockEntity(
            (entity, side) -> entity.getStorage(), DOCK_BE);
    }

    private ModRegistry() {}
}
