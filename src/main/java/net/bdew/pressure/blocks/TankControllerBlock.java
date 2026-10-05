package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public final class TankControllerBlock extends BaseEntityBlock {
    public TankControllerBlock() { super(Properties.of().strength(1.0F).noOcclusion()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TankControllerBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof TankControllerBlockEntity controller) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                if (FluidUtil.interactWithFluidHandler(player, hand, controller.getTank())) return InteractionResult.CONSUME;
                net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, menuPlayer) -> new net.bdew.pressure.blocks.tank.TankMenu(id, inventory, pos),
                        Component.translatable("pressure.gui.tank.title")), data -> {
                    data.writeBlockPos(pos);
                    data.writeNbt(controller.getUpdateTag());
                });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (state.getBlock() != replacement.getBlock() && level.getBlockEntity(pos) instanceof TankControllerBlockEntity controller) {
            if (!level.isClientSide) {
                controller.disconnect();
                for (int i = 0; i < controller.getInventory().getSlots(); i++)
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), controller.getInventory().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, PressureContent.TANK_CONTROLLER_BE.get(), TankControllerBlockEntity::serverTick);
    }
}
