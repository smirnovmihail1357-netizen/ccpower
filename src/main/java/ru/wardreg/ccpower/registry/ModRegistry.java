package ru.wardreg.ccpower.registry;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
import ru.wardreg.ccpower.tesla.TeslaCoilBlock;
import ru.wardreg.ccpower.tesla.TeslaCoilBlockEntity;

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

    public static final DeferredBlock<DockBlock> DOCK = BLOCKS.register("dock",
        () -> new DockBlock(DockBlock.blockProperties()));

    public static final DeferredItem<BlockItem> DOCK_ITEM =
        ITEMS.registerSimpleBlockItem("dock", DOCK);

    public static final Supplier<BlockEntityType<DockBlockEntity>> DOCK_BE =
        BLOCK_ENTITIES.register("dock", () ->
            BlockEntityType.Builder.of(DockBlockEntity::new, DOCK.get()).build(null));

    public static final DeferredBlock<TeslaCoilBlock> TESLA_SMALL = BLOCKS.register("tesla_coil_1x",
        () -> new TeslaCoilBlock(BlockBehaviour.Properties.of()
            .strength(3.0f, 6.0f).requiresCorrectToolForDrops(), false));

    public static final DeferredItem<BlockItem> TESLA_SMALL_ITEM =
        ITEMS.registerSimpleBlockItem("tesla_coil_1x", TESLA_SMALL);

    public static final DeferredBlock<TeslaCoilBlock> TESLA_LARGE = BLOCKS.register("tesla_coil_2x",
        () -> new TeslaCoilBlock(BlockBehaviour.Properties.of()
            .strength(3.0f, 6.0f).requiresCorrectToolForDrops(), true));

    public static final DeferredItem<BlockItem> TESLA_LARGE_ITEM =
        ITEMS.registerSimpleBlockItem("tesla_coil_2x", TESLA_LARGE);

    public static final Supplier<BlockEntityType<TeslaCoilBlockEntity>> TESLA_BE_SMALL =
        BLOCK_ENTITIES.register("tesla_coil_1x", () ->
            BlockEntityType.Builder.of(
                (pos, state) -> new TeslaCoilBlockEntity(pos, state, false), TESLA_SMALL.get()).build(null));

    public static final Supplier<BlockEntityType<TeslaCoilBlockEntity>> TESLA_BE_TALL =
        BLOCK_ENTITIES.register("tesla_coil_2x", () ->
            BlockEntityType.Builder.of(
                (pos, state) -> new TeslaCoilBlockEntity(pos, state, true), TESLA_LARGE.get()).build(null));

    public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ccpower"))
            .icon(() -> new ItemStack(DOCK_ITEM.get()))
            .displayItems((params, output) -> {
                output.accept(DOCK_ITEM.get());
                output.accept(TESLA_SMALL_ITEM.get());
                output.accept(TESLA_LARGE_ITEM.get());
            })
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
        // Expose the dock's internal state to adjacent computers as a CC:T peripheral.
        event.registerBlockEntity(PeripheralCapability.get(), DOCK_BE.get(),
            (dock, side) -> dock.getPeripheral());
        event.registerBlockEntity(PeripheralCapability.get(), TESLA_BE_SMALL.get(),
            (coil, side) -> coil.getPeripheral());
        event.registerBlockEntity(PeripheralCapability.get(), TESLA_BE_TALL.get(),
            (coil, side) -> coil.getPeripheral());
    }

    private ModRegistry() {}
}
