package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.CreativeSourceBlockEntity;
import net.bdew.pressure.blockentity.WaterSourceBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SourceBlock extends Block implements EntityBlock {
    private final boolean creative;

    public SourceBlock(boolean creative) {
        super(Properties.of().strength(1.0F).noOcclusion());
        this.creative = creative;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return creative ? new CreativeSourceBlockEntity(pos, state) : new WaterSourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    public static BlockEntityType<?> type(boolean creative) {
        return creative ? PressureContent.CREATIVE_SOURCE_BE.get() : PressureContent.WATER_SOURCE_BE.get();
    }
}
