package net.bdew.pressure.blocks;

import net.bdew.pressure.api.IPressureConnectableBlock;
import net.bdew.pressure.blockentity.CheckValveBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public final class CheckValveBlock extends BaseEntityBlock implements IPressureConnectableBlock, ReversibleFlowPlacement {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty SIGNAL = BooleanProperty.create("signal");
    public static final BooleanProperty FRONT = BooleanProperty.create("front");
    public static final BooleanProperty BACK = BooleanProperty.create("back");

    public CheckValveBlock() {
        super(Properties.of().strength(2.0F).noOcclusion());
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(SIGNAL, false)
                .setValue(FRONT, false).setValue(BACK, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, SIGNAL, FRONT, BACK); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState().setValue(FACING, ReversibleFlowPlacement.facing(context)),
                context.getLevel(), context.getClickedPos());
    }

    private BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        var helper = net.bdew.pressure.pressurenet.PressureGraph.instance();
        return state.setValue(FRONT, helper.canPipeConnectTo(level, pos.relative(facing), facing.getOpposite()))
                .setValue(BACK, helper.canPipeConnectTo(level, pos.relative(facing.getOpposite()), facing));
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connections(state, level, pos);
    }

    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> Block.box(2, 3.2, 3.2, 14, 12.8, 12.8);
            case Y -> Block.box(3.2, 2, 3.2, 12.8, 14, 12.8);
            case Z -> Block.box(3.2, 3.2, 2, 12.8, 12.8, 14);
        };
    }

    @Override
    public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return side == state.getValue(FACING) || side == state.getValue(FACING).getOpposite();
    }

    @Override public boolean isTraversable(BlockState state, BlockGetter world, BlockPos pos) { return false; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CheckValveBlockEntity(pos, state); }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        BlockState updated = state.setValue(SIGNAL, level.hasNeighborSignal(pos));
        if (updated != state) level.setBlock(pos, updated, 3);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        BlockState updated = state.setValue(SIGNAL, level.hasNeighborSignal(pos));
        if (updated != state) level.setBlock(pos, updated, 3);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return null; }
}
