package ru.wardreg.ccpower.fabric;

import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
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
import ru.wardreg.ccpower.registry.ModRegistryCommon;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/** Fabric content registration: dock block, item, BE, tab and TR Energy lookup. */
public final class ModRegistry {
    public static final ResourceLocation DOCK_ID = id("dock");

    public static final DockBlock DOCK =
        Registry.register(BuiltInRegistries.BLOCK, DOCK_ID, new DockBlock(DockBlock.blockProperties()));
    public static final BlockItem DOCK_ITEM = Registry.register(BuiltInRegistries.ITEM, DOCK_ID,
        new BlockItem(DOCK, new net.minecraft.world.item.Item.Properties()));
    public static final BlockEntityType<DockBlockEntity> DOCK_BE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE, DOCK_ID,
        BlockEntityType.Builder.of(DockBlockEntity::new, DOCK).build(null));
    public static final CreativeModeTab MAIN_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
        id("main"), FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.ccpower"))
            .icon(() -> new ItemStack(DOCK_ITEM))
            .displayItems((params, output) -> output.accept(DOCK_ITEM))
            .build());

    public static void register() {
        ModRegistryCommon.init(() -> DOCK_BE);
        // Expose the dock's internal state to adjacent computers as a CC:T peripheral.
        PeripheralLookup.get().registerForBlockEntity((dock, side) -> dock.getPeripheral(), DOCK_BE);
        EnergyStorage.SIDED.registerForBlockEntity((dock, side) -> new EnergyStorage() {
            public boolean supportsInsertion() { return true; }
            public long insert(long amount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext ctx) {
                return dock.getStorage().receive(amount, false);
            }
            public boolean supportsExtraction() { return false; }
            public long extract(long amount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext ctx) {
                return 0;
            }
            public long getAmount() { return dock.getBuffer(); }
            public long getCapacity() { return dock.getBufferCapacity(); }
        }, DOCK_BE);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CCPowerMod.MODID, path);
    }

    private ModRegistry() {}
}
