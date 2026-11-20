package ru.wardreg.ccpower.dock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import ru.wardreg.ccpower.registry.ModRegistry;

/**
 * The Recharge Dock (drone station). Turtles parked on top of it have their
 * internal RF battery refilled from the dock's buffer, which the dock itself
 * pulls from adjacent FE-providing blocks (and from FE cables pushing into it).
 */
public class DockBlock extends BaseEntityBlock {
    public DockBlock(Properties properties) {
        super(properties);
    }

    // MC 26.x removed the per-block MapCodec/simpleCodec hook entirely;
    // blocks are reconstructed from their properties by the engine.

    /** Block properties shared by registration and construction. */
    public static Properties blockProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5f, 8.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.METAL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModRegistry.DOCK_BE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof DockBlockEntity dock) {
                DockBlockEntity.serverTick(tickerLevel, pos, tickerState, dock);
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock hides itself by default; we want a normal rendered block.
        return RenderShape.MODEL;
    }
}
