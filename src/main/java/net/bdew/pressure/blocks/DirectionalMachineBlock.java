package net.bdew.pressure.blocks;

import net.bdew.pressure.api.IPressureConnectableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public abstract class DirectionalMachineBlock extends BaseEntityBlock implements IPressureConnectableBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected DirectionalMachineBlock() {
        super(Properties.of().strength(2.0F).noOcclusion());
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, this instanceof ReversibleFlowPlacement
                ? ReversibleFlowPlacement.facing(context) : context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return false;
    }
}
