package net.bdew.pressure.blocks;

import net.bdew.pressure.api.IPressureConnectableBlock;
import net.bdew.pressure.blockentity.RouterBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.blocks.router.RouterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class RouterBlock extends BaseEntityBlock implements IPressureConnectableBlock {
    public RouterBlock() { super(Properties.of().strength(2.0F).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RouterBlockEntity(pos, state); }
    @Override public boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return world.getBlockEntity(pos) instanceof RouterBlockEntity router && router.canConnect(side);
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof RouterBlockEntity)) return InteractionResult.PASS;
        if (player.isCrouching()) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new RouterMenu(containerId, inventory, pos),
                    net.minecraft.network.chat.Component.translatable("pressure.gui.router.title")), pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PressureContent.ROUTER_BE.get(), RouterBlockEntity::serverTick);
    }
}
