package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.config.PressureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** Legacy drain voids excess; the sluice retains one bucket and applies backpressure. */
public abstract class WorldFluidOutletBlockEntity extends PressureNodeBlockEntity implements IPressureEject {
    private final boolean drain;
    private final FluidTank buffer;
    private final IFluidHandler input = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return drain ? Integer.MAX_VALUE : 1000; }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) { return tank == 0 && allowed(fluid); }
        @Override public int fill(FluidStack fluid, FluidAction action) { return accept(fluid, action); }
        @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    };
    private final LazyOptional<IFluidHandler> inputCap = LazyOptional.of(() -> input);

    protected WorldFluidOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean drain) {
        super(type, pos, state);
        this.drain = drain;
        buffer = new FluidTank(drain ? Integer.MAX_VALUE : 1000);
    }
    private Direction facing() { return getBlockState().getValue(DirectionalMachineBlock.FACING); }
    private boolean allowed(FluidStack fluid) {
        if (!isFluidAllowed(fluid)) return false;
        return drain || level != null && fluid.getFluid().getFluidType().canBePlacedInLevel(level,worldPosition.relative(facing()),fluid);
    }
    private int accept(FluidStack fluid, FluidAction action) {
        if (level == null || level.isClientSide || !allowed(fluid)) return 0;
        boolean replace = !buffer.isEmpty() && !buffer.getFluid().isFluidEqual(fluid);
        int amount = drain ? fluid.getAmount() : Math.min(fluid.getAmount(),replace?1000:1000-buffer.getFluidAmount());
        if (action.execute()) {
            if (replace) buffer.setFluid(FluidStack.EMPTY);
            buffer.fill(fluid,FluidAction.EXECUTE);
            release();
            setChanged();
        }
        return amount;
    }
    private void release() {
        if (buffer.isEmpty()) return;
        BlockPos target=worldPosition.relative(facing());
        var key=ForgeRegistries.FLUIDS.getKey(buffer.getFluid().getFluid());
        int ratio=0;
        if (drain && PressureConfig.DRAIN_XP.get() && key != null) {
            ratio=switch(key.getPath()) {
                case "xpjuice", "xp_juice" -> PressureConfig.XP_JUICE_RATIO.get();
                case "mobessence", "essence" -> PressureConfig.MOB_ESSENCE_RATIO.get();
                default -> 0;
            };
        }
        if (ratio>0 && level instanceof ServerLevel server) {
            int xp=buffer.getFluidAmount()/ratio;
            buffer.drain(xp*ratio,FluidAction.EXECUTE);
            if(xp>0) ExperienceOrb.award(server,Vec3.atCenterOf(target),xp);
            return;
        }
        if(buffer.getFluidAmount()<1000 || level.isOutsideBuildHeight(target) || !level.getWorldBorder().isWithinBounds(target)
                || !level.isEmptyBlock(target) || !buffer.getFluid().getFluid().getFluidType().canBePlacedInLevel(level,target,buffer.getFluid())) return;
        BlockState placed=buffer.getFluid().getFluid().defaultFluidState().createLegacyBlock();
        if(!placed.isAir() && level.setBlock(target,placed,3)) {
            if(drain) buffer.setFluid(FluidStack.EMPTY); else buffer.drain(1000,FluidAction.EXECUTE);
        }
    }
    @Override public int eject(FluidStack fluid,Direction side,boolean execute) {
        return side == facing().getOpposite() ? accept(fluid,execute?FluidAction.EXECUTE:FluidAction.SIMULATE) : 0;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side) {
        if(capability==ForgeCapabilities.FLUID_HANDLER && side==facing().getOpposite()) return inputCap.cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();inputCap.invalidate();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);buffer.writeToNBT(tag);}
    @Override public void load(CompoundTag tag){super.load(tag);buffer.readFromNBT(tag);}
}
