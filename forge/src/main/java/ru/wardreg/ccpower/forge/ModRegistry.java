package ru.wardreg.ccpower.forge;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.registry.ModRegistryCommon;

/** Forge content registration: dock block, item, BE, tab and FE capability. */
public final class ModRegistry {
    private static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(Registries.BLOCK, CCPowerMod.MODID);
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, CCPowerMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CCPowerMod.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CCPowerMod.MODID);

    private static final RegistryObject<DockBlock> DOCK =
        BLOCKS.register("dock", () -> new DockBlock(DockBlock.blockProperties()));
    private static final RegistryObject<BlockItem> DOCK_ITEM =
        ITEMS.register("dock", () -> new BlockItem(DOCK.get(), new Item.Properties()));
    private static final Supplier<BlockEntityType<ForgeDockBlockEntity>> DOCK_BE =
        BLOCK_ENTITIES.register("dock", () ->
            BlockEntityType.Builder.of(ForgeDockBlockEntity::new, DOCK.get()).build(null));
    private static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
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
        ModRegistryCommon.init(DOCK_BE::get);

    }

    private ModRegistry() {}
}
