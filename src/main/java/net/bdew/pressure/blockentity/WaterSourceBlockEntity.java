package net.bdew.pressure.blockentity;

import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public final class WaterSourceBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private final LazyOptional<IFluidHandler> handler = LazyOptional.of(() -> new InfiniteWaterHandler());

    public WaterSourceBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.WATER_SOURCE_BE.get(), pos, state); }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.FLUID_HANDLER ? handler.cast() : super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); handler.invalidate(); }

    private static final class InfiniteWaterHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return new FluidStack(Fluids.WATER, Integer.MAX_VALUE); }
        @Override public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return stack != null && stack.getFluid() == Fluids.WATER; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource != null && resource.getFluid() == Fluids.WATER ? resource.copy() : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return maxDrain <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, maxDrain);
        }
    }
}
