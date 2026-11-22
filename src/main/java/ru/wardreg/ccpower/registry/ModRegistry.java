package ru.wardreg.ccpower.registry;

import dan200.computercraft.api.ComputerCraftAPI;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;
import ru.wardreg.ccpower.CCPowerMod;
import ru.wardreg.ccpower.dock.DockBlock;
import ru.wardreg.ccpower.dock.DockBlockEntity;

/** Mod content registration for the 1.16.5 Forge era. */
public final class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, CCPowerMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, CCPowerMod.MODID);
    public static final DeferredRegister<TileEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, CCPowerMod.MODID);

    public static final ItemGroup MAIN_TAB = new ItemGroup(CCPowerMod.MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(DOCK_ITEM.get());
        }
    };

    public static final RegistryObject<DockBlock> DOCK = BLOCKS.register("dock",
        () -> new DockBlock(DockBlock.blockProperties()));

    public static final RegistryObject<BlockItem> DOCK_ITEM = ITEMS.register("dock",
        () -> new BlockItem(DOCK.get(), new Item.Properties().tab(MAIN_TAB)));

    public static final RegistryObject<TileEntityType<?>> DOCK_BE =
        BLOCK_ENTITIES.register("dock", () ->
            TileEntityType.Builder.of(DockBlockEntity::new, DOCK.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    /**
     * Expose the dock's internal state to adjacent computers. CC:T 1.97 has no
     * PeripheralCapability: peripherals come from IPeripheralProvider instead.
     */
    public static void registerPeripherals() {
        ComputerCraftAPI.registerPeripheralProvider((world, pos, side) -> {
            TileEntity tile = world.getBlockEntity(pos);
            if (tile instanceof DockBlockEntity) {
                return LazyOptional.of(() -> ((DockBlockEntity) tile).getPeripheral());
            }
            return LazyOptional.empty();
        });
    }

    private ModRegistry() {}
}
