package net.bdew.pressure.blocks;

import net.bdew.pressure.api.IPressureConnectableBlock;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class TankModuleBlock extends BaseEntityBlock implements IPressureConnectableBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final boolean pressureNode;

    public TankModuleBlock(boolean pressureNode) {
        super(Properties.of().strength(1.0F).noOcclusion());
        this.pressureNode = pressureNode;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TankModuleBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side) { return pressureNode; }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PressureContent.TANK_MODULE_BE.get(), TankModuleBlockEntity::serverTick);
    }
}
