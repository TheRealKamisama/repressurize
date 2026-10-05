package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class TankSensorModuleBlock extends TankModuleBlock {
    public static final BooleanProperty SIGNAL = BooleanProperty.create("signal");

    public TankSensorModuleBlock() {
        super(false);
        registerDefaultState(defaultBlockState().setValue(SIGNAL, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIGNAL);
    }

    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter world,
                                   net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
        return state.getValue(SIGNAL) ? 15 : 0;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof TankModuleBlockEntity sensor)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isCrouching()) {
                var threshold = sensor.cycleFillThreshold();
                player.displayClientMessage(Component.translatable("pressure.sensor.threshold.changed",
                        Component.translatable(threshold.translationKey())), true);
            } else {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, menuPlayer) -> new net.bdew.pressure.blocks.tank.SensorMenu(id, inventory, pos),
                            Component.translatable("pressure.gui.sensor.title")), data -> {
                        data.writeBlockPos(pos); data.writeNbt(sensor.getUpdateTag());
                    });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
