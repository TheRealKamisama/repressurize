package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.misc.FluidTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public final class OutputBlockEntity extends PressureNodeBlockEntity implements IPressureEject {
    public OutputBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.OUTPUT_BE.get(), pos, state); }

    @Override
    public int eject(FluidStack resource, Direction sideTowardNetwork, boolean doEject) {
        if (!isFluidAllowed(resource) || getLevel() == null) return 0;
        Direction facing = getBlockState().getValue(DirectionalMachineBlock.FACING);
        if (sideTowardNetwork != facing.getOpposite()) return 0;
        Level level = getLevel();
        IFluidHandler target = FluidTransfer.getHandler(level, getBlockPos().relative(facing), facing.getOpposite());
        return target == null ? 0 : target.fill(resource, doEject ? FluidAction.EXECUTE : FluidAction.SIMULATE);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER)
            return LazyOptional.empty();
        return super.getCapability(capability, side);
    }
}
