package ru.wardreg.ccpower.registry;

import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/**
 * Mod content registration: the Recharge Dock block, its item, block entity
 * and creative tab. The dock's FE capability is provided directly by
 * {@link DockBlockEntity#getCapability} (Forge 1.17/1.18 capability era).
 */
public final class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, CCPowerMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, CCPowerMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, CCPowerMod.MODID);

    public static final RegistryObject<DockBlock> DOCK = BLOCKS.register("dock",
        () -> new DockBlock(DockBlock.blockProperties()));

    /** Pre-1.19.3 creative tab style: a CreativeModeTab subclass with makeIcon(). */
    public static final CreativeModeTab MAIN_TAB = new CreativeModeTab("ccpower") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(DOCK.get());
        }
    };

    public static final RegistryObject<BlockItem> DOCK_ITEM = ITEMS.register("dock",
        () -> new BlockItem(DOCK.get(), new Item.Properties().tab(MAIN_TAB)));

    public static final Supplier<BlockEntityType<DockBlockEntity>> DOCK_BE =
        BLOCK_ENTITIES.register("dock", () ->
            BlockEntityType.Builder.of(DockBlockEntity::new, DOCK.get()).build(null));

    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    private ModRegistry() {}
}
