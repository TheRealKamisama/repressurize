package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureConnection;
import net.bdew.pressure.api.IPressureInject;
import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.config.PressureConfig;
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

public final class InputBlockEntity extends PressureNodeBlockEntity implements IPressureInject {
    private final IFluidHandler fluidHandler = new InputHandler();
    private final LazyOptional<IFluidHandler> fluidHandlerOptional = LazyOptional.of(() -> fluidHandler);
    private IPressureConnection connection;

    public InputBlockEntity(BlockPos pos, BlockState state) {
        super(PressureContent.INPUT_BE.get(), pos, state);
    }

    public Direction getFacing() { return getBlockState().getValue(DirectionalMachineBlock.FACING); }

    @Override
    public void invalidateConnection(Direction direction) {
        connection = null;
    }

    private int push(FluidStack resource, boolean execute) {
        if (!isFluidAllowed(resource)) return 0;
        Level level = getLevel();
        if (level == null || level.isClientSide) return resource.getAmount();
        if (connection == null && PressureAPI.HELPER != null)
            connection = PressureAPI.HELPER.recalculateConnectionInfo(this, getFacing());
        return connection == null ? 0 : connection.pushFluid(resource, execute);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InputBlockEntity entity) {
        if (!level.hasNeighborSignal(pos)) return;
        Direction facing = state.getValue(DirectionalMachineBlock.FACING);
        IFluidHandler source = FluidTransfer.getHandler(level, pos.relative(facing.getOpposite()), facing);
        FluidTransfer.transfer(source, entity.fluidHandler, PressureConfig.FLUID_TRANSFER_PER_TICK.get());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER && side == getFacing().getOpposite())
            return fluidHandlerOptional.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandlerOptional.invalidate();
    }

    private final class InputHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return stack != null && !stack.isEmpty(); }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return isFluidValid(0, resource) ? push(resource, action.execute()) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }
}
