package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.InputBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public final class InputBlock extends DirectionalMachineBlock implements ReversibleFlowPlacement {
    public static final BooleanProperty SIGNAL = BooleanProperty.create("signal");

    public InputBlock() { registerDefaultState(defaultBlockState().setValue(SIGNAL, false)); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIGNAL);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        BlockState updated = state.setValue(SIGNAL, level.hasNeighborSignal(pos));
        if (updated != state) level.setBlock(pos, updated, 3);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!oldState.is(state.getBlock())) level.setBlock(pos, state.setValue(SIGNAL, level.hasNeighborSignal(pos)), 2);
    }

    @Override
    public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.core.Direction side) {
        return state.getValue(FACING) == side;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InputBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PressureContent.INPUT_BE.get(), InputBlockEntity::serverTick);
    }
}
