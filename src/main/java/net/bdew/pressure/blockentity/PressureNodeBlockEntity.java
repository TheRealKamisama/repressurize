package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureNode;
import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.api.properties.IFilterable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public abstract class PressureNodeBlockEntity extends BlockEntity implements IPressureNode {
    private Fluid fluidFilter;
    private final IFilterable filterApi = new IFilterable() {
        @Override public void setFluidFilter(Fluid fluid) { fluidFilter = fluid; syncFilter(); }
        @Override public void clearFluidFilter() { fluidFilter = null; syncFilter(); }
    };
    private final LazyOptional<IFilterable> filterApiOptional = LazyOptional.of(() -> filterApi);

    protected PressureNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public BlockPos pressureNodePos() { return getBlockPos(); }
    @Override public Level pressureNodeWorld() { return getLevel(); }

    protected boolean isFluidAllowed(FluidStack stack) {
        return stack != null && !stack.isEmpty() && (fluidFilter == null || fluidFilter == stack.getFluid());
    }

    public Fluid getFluidFilter() { return fluidFilter; }

    private void syncFilter() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == PressureAPI.FILTERABLE) return filterApiOptional.cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); filterApiOptional.invalidate(); }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        requestModelDataUpdate();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public void onDataPacket(net.minecraft.network.Connection connection,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (fluidFilter != null) {
            var id = ForgeRegistries.FLUIDS.getKey(fluidFilter);
            if (id != null) tag.putString("fluidFilter", id.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("fluidFilter")) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("fluidFilter"));
            fluidFilter = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
            if (fluidFilter == net.minecraft.world.level.material.Fluids.EMPTY) fluidFilter = null;
        } else fluidFilter = null;
    }
}
