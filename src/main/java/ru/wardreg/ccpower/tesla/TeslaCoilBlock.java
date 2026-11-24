package ru.wardreg.ccpower.tesla;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Tesla Coil block: charges portable CC computers in its radius on a timer. */
public class TeslaCoilBlock extends BaseEntityBlock {
    private final boolean tall;

    public TeslaCoilBlock(Properties properties, boolean tall) {
        super(properties);
        this.tall = tall;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // Both variants share a codec; the tall flag comes from the block entity type.
        return simpleCodec(properties -> new TeslaCoilBlock(properties, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeslaCoilBlockEntity(pos, state, tall);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof TeslaCoilBlockEntity coil) {
                TeslaCoilBlockEntity.serverTick(tickerLevel, pos, coil);
            }
        };
    }
}
