package net.bdew.pressure.blockentity;

import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class DrainBlockEntity extends WorldFluidOutletBlockEntity {
    public DrainBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.DRAIN_BE.get(), pos, state, true); }
}
