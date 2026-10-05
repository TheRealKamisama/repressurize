package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.PipeSensorBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class PipeSensorBlock extends BaseEntityBlock implements net.bdew.pressure.api.IPressureConnectableBlock, ReversibleFlowPlacement {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty SIGNAL = BooleanProperty.create("signal");
    public static final BooleanProperty FRONT = BooleanProperty.create("front");
    public static final BooleanProperty BACK = BooleanProperty.create("back");

    public PipeSensorBlock() {
        super(Properties.of().strength(2.0F).noOcclusion());
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
                .setValue(SIGNAL, false).setValue(FRONT, false).setValue(BACK, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING, SIGNAL, FRONT, BACK); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return connections(defaultBlockState().setValue(FACING, ReversibleFlowPlacement.facing(context)),
                context.getLevel(), context.getClickedPos());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PipeSensorBlockEntity(pos, state); }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connections(state, level, pos);
    }

    private BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(FRONT, PressureGraph.instance().canPipeConnectTo(level, pos.relative(facing), facing.getOpposite()))
                .setValue(BACK, PressureGraph.instance().canPipeConnectTo(level, pos.relative(facing.getOpposite()), facing));
    }

    @Override public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        Direction facing = state.getValue(FACING);
        return side == facing || side == facing.getOpposite();
    }

    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        Direction facing = state.getValue(FACING);
        return state.getValue(SIGNAL) && side != facing && side != facing.getOpposite() ? 15 : 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (facing.getAxis() == Direction.Axis.X) return Shapes.create(0.125, 0.2, 0.2, 0.875, 0.8, 0.8);
        if (facing.getAxis() == Direction.Axis.Y) return Shapes.create(0.2, 0.125, 0.2, 0.8, 0.875, 0.8);
        return Shapes.create(0.2, 0.2, 0.125, 0.8, 0.8, 0.875);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PipeSensorBlockEntity sensor)
            player.displayClientMessage(Component.literal(sensor.getSummary()), false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PressureContent.PIPE_SENSOR_BE.get(), PipeSensorBlockEntity::serverTick);
    }
}
