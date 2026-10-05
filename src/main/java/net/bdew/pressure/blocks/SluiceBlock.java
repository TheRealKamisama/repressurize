package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.SluiceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SluiceBlock extends DirectionalMachineBlock {
    @Override
    public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return state.getValue(FACING).getOpposite() == side;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SluiceBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return null; }
}
