package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureConnection;
import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.api.IPressureInject;
import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.api.properties.IFilterable;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.blocks.router.RouterSideMode;
import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.misc.FluidTransfer;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Server state and fluid routing for the six independently configurable router faces. */
public final class RouterBlockEntity extends PressureNodeBlockEntity implements IPressureInject, IPressureEject {
    private static final ThreadLocal<Set<BlockPos>> ROUTING = ThreadLocal.withInitial(HashSet::new);

    private final Map<Direction, RouterSideMode> sideModes = new EnumMap<>(Direction.class);
    private final Map<Direction, RouterRedstoneMode> redstoneModes = new EnumMap<>(Direction.class);
    private final Map<Direction, Fluid> sideFilters = new EnumMap<>(Direction.class);
    private final Map<Direction, IPressureConnection> connections = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IFluidHandler>> handlers = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IFilterable>> filterCapabilities = new EnumMap<>(Direction.class);

    public RouterBlockEntity(BlockPos pos, BlockState state) {
        super(PressureContent.ROUTER_BE.get(), pos, state);
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, RouterSideMode.DISABLED);
            redstoneModes.put(direction, RouterRedstoneMode.ALWAYS);
        }
    }

    public RouterSideMode getSideMode(Direction side) {
        return sideModes.getOrDefault(side, RouterSideMode.DISABLED);
    }

    public RouterRedstoneMode getRedstoneMode(Direction side) {
        return redstoneModes.getOrDefault(side, RouterRedstoneMode.ALWAYS);
    }

    @Nullable public Fluid getSideFilter(Direction side) {
        return sideFilters.get(side);
    }

    public boolean canConnect(Direction side) {
        return getSideMode(side) != RouterSideMode.DISABLED;
    }

    public RouterSideMode cycleSideMode(Direction side) {
        RouterSideMode next = getSideMode(side).next();
        sideModes.put(side, next);
        configurationChanged(true);
        return next;
    }

    public RouterRedstoneMode cycleRedstoneMode(Direction side) {
        RouterRedstoneMode next = getRedstoneMode(side).next();
        redstoneModes.put(side, next);
        configurationChanged(false);
        return next;
    }

    private void setSideFilter(Direction side, @Nullable Fluid fluid) {
        if (fluid == null) sideFilters.remove(side);
        else sideFilters.put(side, fluid);
        configurationChanged(false);
    }

    private void configurationChanged(boolean topologyChanged) {
        connections.clear();
        setChanged();
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        if (topologyChanged) {
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            PressureGraph.invalidateNear(level, getBlockPos());
        }
    }

    private boolean canWork(Direction side) {
        Level level = getLevel();
        return level != null && getRedstoneMode(side).allows(level.hasNeighborSignal(getBlockPos()));
    }

    private boolean isFluidAllowed(Direction side, FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Fluid filter = sideFilters.get(side);
        return filter == null || filter == stack.getFluid();
    }

    @Override
    public int eject(FluidStack resource, Direction sideTowardNetwork, boolean doEject) {
        if (sideTowardNetwork == null || !getSideMode(sideTowardNetwork).acceptsInput()
                || !canWork(sideTowardNetwork) || !isFluidAllowed(sideTowardNetwork, resource)) return 0;
        return distribute(resource, sideTowardNetwork, doEject);
    }

    private int distribute(FluidStack resource, @Nullable Direction ingress, boolean execute) {
        if (resource == null || resource.isEmpty() || getLevel() == null || getLevel().isClientSide) return 0;
        Set<BlockPos> active = ROUTING.get();
        if (!active.add(getBlockPos())) return 0;
        try {
            int moved = 0;
            int remaining = resource.getAmount();
            for (RouterSideMode mode : new RouterSideMode[]{
                    RouterSideMode.OUTPUT_HIGH, RouterSideMode.OUTPUT_MEDIUM, RouterSideMode.OUTPUT_LOW}) {
                for (Direction direction : Direction.values()) {
                    if (remaining <= 0) break;
                    if (direction == ingress || getSideMode(direction) != mode || !canWork(direction)
                            || !isFluidAllowed(direction, resource)) continue;
                    int sent = pushSide(resource, direction, remaining, execute);
                    moved += sent;
                    remaining -= sent;
                }
            }
            return moved;
        } finally {
            active.remove(getBlockPos());
            if (active.isEmpty()) ROUTING.remove();
        }
    }

    private int pushSide(FluidStack resource, Direction direction, int maxAmount, boolean execute) {
        Level level = getLevel();
        BlockPos targetPos = getBlockPos().relative(direction);
        IFluidHandler direct = FluidTransfer.getHandler(level, targetPos, direction.getOpposite());
        FluidAction action = execute ? FluidAction.EXECUTE : FluidAction.SIMULATE;
        if (direct != null) {
            FluidStack copy = resource.copy();
            copy.setAmount(maxAmount);
            int accepted = direct.fill(copy, action);
            if (accepted > 0) return Math.min(maxAmount, accepted);
        }

        if (!PressureGraph.canConnect(level, getBlockPos(), direction)) return 0;
        IPressureConnection connection = connections.computeIfAbsent(direction,
                side -> PressureGraph.instance().recalculateConnectionInfo(this, side));
        FluidStack copy = resource.copy();
        copy.setAmount(maxAmount);
        return Math.min(maxAmount, connection.pushFluid(copy, execute));
    }

    @Override
    public void invalidateConnection(Direction direction) {
        if (direction == null) connections.clear();
        else connections.remove(direction);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == PressureAPI.FILTERABLE) {
            if (side == null) return LazyOptional.empty();
            return filterCapabilities.computeIfAbsent(side, direction -> LazyOptional.of(() -> new IFilterable() {
                @Override public void setFluidFilter(Fluid fluid) { setSideFilter(direction, fluid); }
                @Override public void clearFluidFilter() { setSideFilter(direction, null); }
            })).cast();
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER && side != null)
            return handlers.computeIfAbsent(side, direction -> LazyOptional.of(() -> new RouterSideHandler(this, direction))).cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlers.values().forEach(LazyOptional::invalidate);
        handlers.clear();
        filterCapabilities.values().forEach(LazyOptional::invalidate);
        filterCapabilities.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag modes = new CompoundTag();
        CompoundTag redstone = new CompoundTag();
        CompoundTag filters = new CompoundTag();
        for (Direction direction : Direction.values()) {
            String key = Integer.toString(direction.get3DDataValue());
            modes.putString(key, getSideMode(direction).name());
            redstone.putString(key, getRedstoneMode(direction).name());
            Fluid fluid = sideFilters.get(direction);
            ResourceLocation id = fluid == null ? null : ForgeRegistries.FLUIDS.getKey(fluid);
            if (id != null) filters.putString(key, id.toString());
        }
        tag.put("routerModes", modes);
        tag.put("routerRedstone", redstone);
        tag.put("routerFilters", filters);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (Direction direction : Direction.values()) {
            String key = Integer.toString(direction.get3DDataValue());
            sideModes.put(direction, parseMode(tag.getCompound("routerModes").getString(key)));
            redstoneModes.put(direction, parseRedstoneMode(tag.getCompound("routerRedstone").getString(key)));
            String idText = tag.getCompound("routerFilters").getString(key);
            ResourceLocation id = ResourceLocation.tryParse(idText);
            Fluid fluid = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
            if (idText.isEmpty() || fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) sideFilters.remove(direction);
            else sideFilters.put(direction, fluid);
        }
        connections.clear();
    }

    private static RouterSideMode parseMode(String value) {
        try { return RouterSideMode.valueOf(value); }
        catch (IllegalArgumentException ignored) { return RouterSideMode.DISABLED; }
    }

    private static RouterRedstoneMode parseRedstoneMode(String value) {
        try { return RouterRedstoneMode.valueOf(value); }
        catch (IllegalArgumentException ignored) { return RouterRedstoneMode.ALWAYS; }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RouterBlockEntity entity) {
        int budget = PressureConfig.FLUID_TRANSFER_PER_TICK.get();
        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            if (entity.getSideMode(direction) != RouterSideMode.INPUT_ACTIVE || !entity.canWork(direction)) continue;
            IFluidHandler source = FluidTransfer.getHandler(level, pos.relative(direction), direction.getOpposite());
            if (source == null) continue;

            Fluid filter = entity.sideFilters.get(direction);
            FluidStack simulated = filter == null
                    ? source.drain(budget, FluidAction.SIMULATE)
                    : source.drain(new FluidStack(filter, budget), FluidAction.SIMULATE);
            if (simulated.isEmpty() || !entity.isFluidAllowed(direction, simulated)) continue;
            int accepted = entity.distribute(simulated, direction, false);
            if (accepted <= 0) continue;

            FluidStack toMove = filter == null
                    ? source.drain(accepted, FluidAction.SIMULATE)
                    : source.drain(new FluidStack(filter, accepted), FluidAction.SIMULATE);
            if (toMove.isEmpty()) continue;
            int moved = entity.distribute(toMove, direction, true);
            if (moved > 0) {
                FluidStack drained = filter == null
                        ? source.drain(moved, FluidAction.EXECUTE)
                        : source.drain(new FluidStack(filter, moved), FluidAction.EXECUTE);
                budget -= drained.isEmpty() ? 0 : drained.getAmount();
            }
        }
    }

    private static final class RouterSideHandler implements IFluidHandler {
        private final RouterBlockEntity router;
        private final Direction side;

        private RouterSideHandler(RouterBlockEntity router, Direction side) {
            this.router = router;
            this.side = side;
        }

        private boolean canInput(FluidStack stack) {
            return router.getSideMode(side).acceptsInput() && router.canWork(side)
                    && router.isFluidAllowed(side, stack);
        }

        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return canInput(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return canInput(resource) ? router.distribute(resource, side, action.execute()) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }
}
