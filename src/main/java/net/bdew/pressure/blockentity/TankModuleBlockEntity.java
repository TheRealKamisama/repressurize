package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureConnection;
import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.api.IPressureInject;
import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.blocks.TankSensorModuleBlock;
import net.bdew.pressure.blocks.TankModuleBlock;
import net.bdew.pressure.blocks.tank.TankFillThreshold;
import net.bdew.pressure.blocks.tank.TankSensorMode;
import net.bdew.pressure.misc.FluidTransfer;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TankModuleBlockEntity extends PressureNodeBlockEntity implements IPressureInject, IPressureEject {
    private final Map<Direction, IPressureConnection> connections = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IFluidHandler>> caps = new EnumMap<>(Direction.class);
    private int ticks;
    private BlockPos controllerPos;
    private final LazyOptional<net.minecraftforge.items.IItemHandler> itemInterface = LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
        private net.minecraftforge.items.ItemStackHandler inventory() {
            TankControllerBlockEntity core = tankController();
            return core == null ? null : core.getInventory();
        }
        @Override public int getSlots() { return 2; }
        @Override public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            var inv = inventory(); return inv == null ? net.minecraft.world.item.ItemStack.EMPTY : inv.getStackInSlot(slot);
        }
        @Override public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
            var inv = inventory(); return inv == null || slot != 0 ? stack : inv.insertItem(slot, stack, simulate);
        }
        @Override public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            var inv = inventory(); return inv == null || slot != 1 ? net.minecraft.world.item.ItemStack.EMPTY : inv.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            var inv = inventory(); return inv != null && slot == 0 && inv.isItemValid(slot, stack);
        }
    });
    private TankSensorMode sensorMode = TankSensorMode.DISABLED;
    private TankFillThreshold fillThreshold = TankFillThreshold.GT_50;
    private int sensorOutput = -1;

    public TankModuleBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.TANK_MODULE_BE.get(), pos, state); }
    protected TankModuleBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    public boolean isOutputConnected(Direction side) {
        return moduleName().equals("tank_pressure_output") ? PressureGraph.canConnect(level, worldPosition, side)
                : FluidTransfer.getHandler(level, worldPosition.relative(side), side.getOpposite()) != null;
    }

    public String moduleName() {
        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(getBlockState().getBlock());
        return id == null ? "" : id.getPath();
    }

    public BlockPos getControllerPos() { return controllerPos; }

    public void setControllerPos(@Nullable BlockPos pos) {
        if (java.util.Objects.equals(controllerPos, pos)) return;
        controllerPos = pos;
        connections.clear();
        syncConfiguration();
        requestModelDataUpdate();
        if (level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            PressureGraph.invalidateNear(level, worldPosition);
        }
    }

    public TankControllerBlockEntity tankController() {
        if (level == null || controllerPos == null || !level.hasChunkAt(controllerPos)) return null;
        return level.getBlockEntity(controllerPos) instanceof TankControllerBlockEntity core ? core : null;
    }

    private void recordOutput(int amount) {
        TankControllerBlockEntity core = tankController();
        if (core != null) core.recordOutput(amount);
    }

    public TankSensorMode getSensorMode() { return sensorMode; }
    public TankFillThreshold getFillThreshold() { return fillThreshold; }
    public int getSensorOutput() { return sensorOutput; }
    public void cycleSensorOutput() { sensorOutput = (sensorOutput + 2) % 7 - 1; syncConfiguration(); }

    public TankSensorMode cycleSensorMode() {
        sensorMode = sensorMode.next();
        syncConfiguration();
        return sensorMode;
    }

    public TankFillThreshold cycleFillThreshold() {
        fillThreshold = fillThreshold.next();
        syncConfiguration();
        return fillThreshold;
    }

    private void syncConfiguration() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    public int eject(FluidStack resource, Direction sideTowardNetwork, boolean doEject) {
        if (!moduleName().equals("tank_pressure_input") || !isFluidAllowed(resource)) return 0;
        TankControllerBlockEntity controller = tankController();
        return controller == null ? 0 : controller.getTank().fill(resource, doEject ? FluidAction.EXECUTE : FluidAction.SIMULATE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankModuleBlockEntity entity) {
        String module = entity.moduleName();
        if (!module.equals("tank_pressure_output") && !module.equals("tank_fluid_output_auto") && !module.equals("tank_sensor")) return;
        if (module.equals("tank_sensor")) {
            if (++entity.ticks < 20) return;
            entity.ticks = 0;
        }
        TankControllerBlockEntity controller = entity.tankController();
        if (controller == null) {
            if (module.equals("tank_sensor") && state.getValue(TankSensorModuleBlock.SIGNAL))
                level.setBlock(pos, state.setValue(TankSensorModuleBlock.SIGNAL, false), 3);
            return;
        }
        var tank = controller.getTank();
        if (module.equals("tank_sensor")) {
            int capacity = tank.getCapacity();
            boolean signal = switch (entity.sensorMode) {
                case DISABLED -> false;
                case TANK_FILL -> entity.fillThreshold.matches(tank.getFluidAmount(), capacity);
                case OUTPUT_FLOW -> entity.sensorOutput < 0 ? controller.isOutputFlowing()
                        : controller.getOutput(entity.sensorOutput) != null && controller.getOutput(entity.sensorOutput).average > 0.1F;
                case FLUID_MATCH -> !tank.getFluid().isEmpty()
                        && entity.getFluidFilter() != null
                        && tank.getFluid().getFluid() == entity.getFluidFilter();
            };
            if (state.getValue(TankSensorModuleBlock.SIGNAL) != signal)
                level.setBlock(pos, state.setValue(TankSensorModuleBlock.SIGNAL, signal), 3);
            return;
        }
        if (module.equals("tank_fluid_output_auto")) {
            for (Direction facing : Direction.values()) {
                if (!controller.canOutput(pos, facing) || tank.getFluid().isEmpty()) continue;
                IFluidHandler target = FluidTransfer.getHandler(level, pos.relative(facing), facing.getOpposite());
                int moved = FluidTransfer.transfer(tank, target, net.bdew.pressure.config.PressureConfig.FLUID_TRANSFER_PER_TICK.get());
                controller.recordOutput(pos, facing, moved);
            }
            return;
        }
        if (tank.getFluid().isEmpty()) return;
        for (Direction direction : Direction.values()) {
            if (!controller.canOutput(pos, direction) || !PressureGraph.canConnect(level, pos, direction)) continue;
            IPressureConnection connection = entity.connections.computeIfAbsent(direction,
                    side -> PressureGraph.instance().recalculateConnectionInfo(entity, side));
            int moved = connection.pushFluid(tank.getFluid().copy(), true);
            if (moved > 0) {
                tank.drain(moved, FluidAction.EXECUTE);
                controller.recordOutput(pos, direction, moved);
            }
        }
    }

    @Override public void invalidateConnection(Direction direction) {
        if (direction == null) connections.clear(); else connections.remove(direction);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && moduleName().equals("tank_interface")) return itemInterface.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            String name = moduleName();
            if (side == null && (name.equals("tank_fluid_output") || name.equals("tank_fluid_output_auto"))) return LazyOptional.empty();
            if (name.equals("tank_fluid_input") || name.equals("tank_fluid_output")
                    || name.equals("tank_fluid_output_auto") || name.equals("tank_fluid_access") || name.equals("tank_interface")) {
                return caps.computeIfAbsent(side == null ? Direction.UP : side, d -> LazyOptional.of(() -> new TankModuleFluidHandler(this, d))).cast();
            }
        }
        if (capability == PressureAPI.FILTERABLE && moduleName().equals("tank_filter")) {
            TankControllerBlockEntity core = tankController();
            if (core != null) return core.getCapability(capability, side);
        }
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); caps.values().forEach(LazyOptional::invalidate); caps.clear(); itemInterface.invalidate(); }

    private static final class TankModuleFluidHandler implements IFluidHandler {
        private final TankModuleBlockEntity module;
        private final Direction side;
        TankModuleFluidHandler(TankModuleBlockEntity module, Direction side) { this.module = module; this.side = side; }
        private IFluidHandler delegate() {
            TankControllerBlockEntity core = module.tankController();
            return core == null ? null : core.getTank();
        }
        private String name() { return module.moduleName(); }
        private boolean canFill() { return name().equals("tank_fluid_input") || name().equals("tank_fluid_access") || name().equals("tank_interface"); }
        private boolean canDrain() {
            if (name().equals("tank_fluid_access") || name().equals("tank_interface")) return true;
            TankControllerBlockEntity core = module.tankController();
            return (name().equals("tank_fluid_output") || name().equals("tank_fluid_output_auto"))
                    && core != null && core.canOutput(module.getBlockPos(), side);
        }
        @Override public int getTanks() { IFluidHandler h = delegate(); return h == null ? 0 : h.getTanks(); }
        @Override public FluidStack getFluidInTank(int tank) { IFluidHandler h = delegate(); return h == null ? FluidStack.EMPTY : h.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { IFluidHandler h = delegate(); return h == null ? 0 : h.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { IFluidHandler h = delegate(); return h != null && h.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) {
            IFluidHandler h = delegate();
            return canFill() && h != null && module.isFluidAllowed(resource) ? h.fill(resource, action) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            IFluidHandler h = delegate();
            if (!canDrain() || h == null) return FluidStack.EMPTY;
            FluidStack drained = h.drain(resource, action);
            if (action.execute() && !drained.isEmpty()) module.tankController().recordOutput(module.getBlockPos(), side, drained.getAmount());
            return drained;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler h = delegate();
            if (!canDrain() || h == null) return FluidStack.EMPTY;
            FluidStack drained = h.drain(maxDrain, action);
            if (action.execute() && !drained.isEmpty()) module.tankController().recordOutput(module.getBlockPos(), side, drained.getAmount());
            return drained;
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (controllerPos != null) tag.putLong("controller", controllerPos.asLong());
        tag.putInt("ticks", ticks);
        tag.putString("sensorMode", sensorMode.name());
        tag.putString("fillThreshold", fillThreshold.name());
        tag.putInt("sensorOutput", sensorOutput);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        controllerPos = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
        ticks = tag.getInt("ticks");
        sensorOutput = tag.contains("sensorOutput") ? Math.max(-1, Math.min(5, tag.getInt("sensorOutput"))) : -1;
        try { sensorMode = TankSensorMode.valueOf(tag.getString("sensorMode")); }
        catch (IllegalArgumentException ignored) { sensorMode = TankSensorMode.DISABLED; }
        try { fillThreshold = TankFillThreshold.valueOf(tag.getString("fillThreshold")); }
        catch (IllegalArgumentException ignored) { fillThreshold = TankFillThreshold.GT_50; }
    }
}
