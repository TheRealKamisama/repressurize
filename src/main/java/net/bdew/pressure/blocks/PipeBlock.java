package net.bdew.pressure.blocks;

import net.bdew.pressure.api.IPressureConnectableBlock;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PipeBlock extends Block implements IPressureConnectableBlock {
    public enum Straight implements StringRepresentable {
        X("x"), Y("y"), Z("z"), NONE("none");

        private final String name;
        Straight(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<Straight> STRAIGHT = EnumProperty.create("straight", Straight.class);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final BooleanProperty[] CONNECTIONS = {NORTH, SOUTH, EAST, WEST, UP, DOWN};

    public PipeBlock() {
        super(Properties.of().strength(2.0F).noOcclusion());
        registerDefaultState(defaultBlockState()
                .setValue(STRAIGHT, Straight.NONE)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STRAIGHT, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    public boolean isTraversable(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return updateConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return updateConnections(state, level, pos);
    }

    private BlockState updateConnections(BlockState state, BlockGetter world, BlockPos pos) {
        var result = state;
        boolean north = connectsAt(state, world, pos, Direction.NORTH);
        boolean south = connectsAt(state, world, pos, Direction.SOUTH);
        boolean east = connectsAt(state, world, pos, Direction.EAST);
        boolean west = connectsAt(state, world, pos, Direction.WEST);
        boolean up = connectsAt(state, world, pos, Direction.UP);
        boolean down = connectsAt(state, world, pos, Direction.DOWN);
        Straight straight = Straight.NONE;
        if (east && west && !north && !south && !up && !down) straight = Straight.X;
        else if (up && down && !east && !west && !north && !south) straight = Straight.Y;
        else if (north && south && !east && !west && !up && !down) straight = Straight.Z;
        if (straight == Straight.NONE) {
            result = result.setValue(NORTH, north).setValue(SOUTH, south)
                    .setValue(EAST, east).setValue(WEST, west).setValue(UP, up).setValue(DOWN, down);
        } else {
            result = result.setValue(NORTH, false).setValue(SOUTH, false)
                    .setValue(EAST, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false);
        }
        return result.setValue(STRAIGHT, straight);
    }

    private boolean connectsAt(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        BlockPos neighborPos = pos.relative(side);
        BlockState neighborState = world.getBlockState(neighborPos);
        return canConnectTo(state, world, pos, side)
                && neighborState.getBlock() instanceof IPressureConnectableBlock neighbor
                && neighbor.canConnectTo(neighborState, world, neighborPos, side.getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        double minX = connectsAt(state, world, pos, Direction.WEST) ? 0 : 0.2;
        double maxX = connectsAt(state, world, pos, Direction.EAST) ? 1 : 0.8;
        double minY = connectsAt(state, world, pos, Direction.DOWN) ? 0 : 0.2;
        double maxY = connectsAt(state, world, pos, Direction.UP) ? 1 : 0.8;
        double minZ = connectsAt(state, world, pos, Direction.NORTH) ? 0 : 0.2;
        double maxZ = connectsAt(state, world, pos, Direction.SOUTH) ? 1 : 0.8;
        return Shapes.create(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
