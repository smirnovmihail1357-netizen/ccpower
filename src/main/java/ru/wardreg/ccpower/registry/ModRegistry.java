package ru.wardreg.ccpower.registry;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.Registry;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/** Mod content registration: the Recharge Dock block, its item and block entity. */
public final class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, CCPowerMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, CCPowerMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, CCPowerMod.MODID);

    public static final RegistryObject<Block> DOCK = BLOCKS.register("dock",
        () -> new DockBlock(DockBlock.blockProperties()));

    public static final RegistryObject<Item> DOCK_ITEM =
        ITEMS.register("dock", () -> new BlockItem(DOCK.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<DockBlockEntity>> DOCK_BE =
        BLOCK_ENTITIES.register("dock", () ->
            BlockEntityType.Builder.of(DockBlockEntity::new, DOCK.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    private ModRegistry() {}
}
