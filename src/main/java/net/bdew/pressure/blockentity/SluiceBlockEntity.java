package net.bdew.pressure.blockentity;

import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class SluiceBlockEntity extends WorldFluidOutletBlockEntity {
    public SluiceBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.SLUICE_BE.get(), pos, state, false); }
}
