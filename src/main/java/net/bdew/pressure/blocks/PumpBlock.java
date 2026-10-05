package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.PumpBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public final class PumpBlock extends DirectionalMachineBlock {
    public static final BooleanProperty SIGNAL = BooleanProperty.create("signal");

    public PumpBlock() {
        registerDefaultState(defaultBlockState().setValue(SIGNAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIGNAL);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        BlockState updated = state.setValue(SIGNAL, level.hasNeighborSignal(pos));
        if (updated != state) level.setBlock(pos, updated, 3);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!oldState.is(state.getBlock())) level.setBlock(pos, state.setValue(SIGNAL, level.hasNeighborSignal(pos)), 2);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PressureContent.PUMP_BE.get(), PumpBlockEntity::serverTick);
    }
}
