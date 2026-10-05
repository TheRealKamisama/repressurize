package net.bdew.pressure.blockentity;

import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.misc.FluidTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public final class PumpBlockEntity extends PressureNodeBlockEntity {
    private boolean forwarding;
    private final IFluidHandler input = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) { return tank == 0 && isFluidAllowed(fluid); }
        @Override public int fill(FluidStack fluid, FluidAction action) {
            if (forwarding || level == null || level.isClientSide || !isFluidAllowed(fluid)) return 0;
            Direction facing = getBlockState().getValue(DirectionalMachineBlock.FACING);
            forwarding = true;
            try {
                var target = FluidTransfer.getHandler(level, worldPosition.relative(facing), facing.getOpposite());
                return target == null ? 0 : target.fill(fluid, action);
            } finally { forwarding = false; }
        }
        @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    };
    private final LazyOptional<IFluidHandler> inputCap = LazyOptional.of(() -> input);
    public PumpBlockEntity(BlockPos pos, BlockState state) { super(net.bdew.pressure.config.PressureContent.PUMP_BE.get(), pos, state); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpBlockEntity entity) {
        if (!level.hasNeighborSignal(pos)) return;
        Direction facing = state.getValue(DirectionalMachineBlock.FACING);
        IFluidHandler source = FluidTransfer.getHandler(level, pos.relative(facing.getOpposite()), facing);
        FluidTransfer.transfer(source, entity.input, net.bdew.pressure.config.PressureConfig.FLUID_TRANSFER_PER_TICK.get());
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @org.jetbrains.annotations.Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER && side == getBlockState().getValue(DirectionalMachineBlock.FACING).getOpposite()) return inputCap.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); inputCap.invalidate(); }
}
