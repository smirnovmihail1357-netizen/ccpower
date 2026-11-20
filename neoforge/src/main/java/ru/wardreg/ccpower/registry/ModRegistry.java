package ru.wardreg.ccpower.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/**
 * Mod content registration: the Recharge Dock block, its item, block entity,
 * creative tab and the FE capability.
 */
public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(CCPowerMod.MODID);
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CCPowerMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CCPowerMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CCPowerMod.MODID);

    // MC 1.21.6+ requires BlockBehaviour.Properties.setId() before construction;
    // registerBlock() applies the registry id to the properties for us.
    public static final DeferredBlock<DockBlock> DOCK = BLOCKS.registerBlock("dock",
        DockBlock::new, DockBlock.blockProperties());

    public static final DeferredItem<BlockItem> DOCK_ITEM =
        ITEMS.registerSimpleBlockItem("dock", DOCK);

    public static final Supplier<BlockEntityType<DockBlockEntity>> DOCK_BE =
        BLOCK_ENTITIES.register("dock", () ->
            new BlockEntityType<DockBlockEntity>(DockBlockEntity::new, DOCK.get()));

    public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ccpower"))
            .icon(() -> new ItemStack(DOCK_ITEM.get()))
            .displayItems((params, output) -> output.accept(DOCK_ITEM.get()))
            .build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
    }

    /**
     * Expose the dock's internal buffer as a block FE capability so external
     * cables and machines can push energy into it.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DOCK_BE.get(),
            (dock, side) -> dock.getStorage());
    }

    private ModRegistry() {}
}
