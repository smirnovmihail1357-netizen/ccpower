package ru.wardreg.ccpower.dock;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import ru.wardreg.ccpower.registry.ModRegistry;

/**
 * The Recharge Dock (drone station). Turtles parked on top of it have their
 * internal RF battery refilled from the dock's buffer.
 */
public class DockBlock extends Block {
    public DockBlock(Block.Properties properties) {
        super(properties);
    }

    /** Block properties shared by registration and construction.
     * 1.15.2 has no requiresCorrectToolForDrops() (added in 1.16). */
    public static Block.Properties blockProperties() {
        return Block.Properties.of(Material.METAL, MaterialColor.METAL)
            .strength(3.5f, 8.0f)
            .sound(SoundType.METAL);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new DockBlockEntity();
    }
}
