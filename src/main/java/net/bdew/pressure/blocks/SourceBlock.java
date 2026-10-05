package net.bdew.pressure.blocks;

import net.bdew.pressure.blockentity.CreativeSourceBlockEntity;
import net.bdew.pressure.blockentity.WaterSourceBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SourceBlock extends Block implements EntityBlock {
    private final boolean creative;

    public SourceBlock(boolean creative) {
        super(Properties.of().strength(1.0F).noOcclusion());
        this.creative = creative;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return creative ? new CreativeSourceBlockEntity(pos, state) : new WaterSourceBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        var held = player.getItemInHand(hand);
        if (creative || !held.is(net.minecraft.world.item.Items.BUCKET)) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) {
            var filled = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
            player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(held, player,
                    filled.copy()));
            // Vanilla's creative helper cannot insert into a full inventory. Deliver the result
            // as a drop in that case, while retaining its duplicate-bucket suppression.
            if (player.getAbilities().instabuild && !player.getInventory().contains(filled)) player.drop(filled, false);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.BUCKET));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            player.getInventory().setChanged();
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    public static BlockEntityType<?> type(boolean creative) {
        return creative ? PressureContent.CREATIVE_SOURCE_BE.get() : PressureContent.WATER_SOURCE_BE.get();
    }
}
