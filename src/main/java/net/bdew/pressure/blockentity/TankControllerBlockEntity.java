package net.bdew.pressure.blockentity;

import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.blocks.tank.TankStructure;
import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.misc.FluidTransfer;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class TankControllerBlockEntity extends PressureNodeBlockEntity {
    private int capacity;
    private int ticks;
    private boolean pendingSync;
    private boolean structureLoaded;
    private long lastStructureTick = Long.MIN_VALUE;
    private long lastOutputTick = Long.MIN_VALUE;
    private Set<BlockPos> members = new LinkedHashSet<>();
    private final Output[] outputs = new Output[6];
    private final Map<String, Long> countsIn = new HashMap<>(), countsOut = new HashMap<>();
    private final FluidTank tank = new FluidTank(0) {
        @Override public int getCapacity() { return capacity; }
        @Override public boolean isFluidValid(FluidStack stack) { return isFluidAllowed(stack); }
        @Override public int fill(FluidStack stack, FluidAction action) {
            int filled = isReady() ? super.fill(stack, action) : 0;
            if (action.execute() && filled > 0) recordCount(countsIn, stack, filled);
            return filled;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            FluidStack drained = isReady() ? super.drain(amount, action) : FluidStack.EMPTY;
            if (action.execute() && !drained.isEmpty()) recordCount(countsOut, drained, drained.getAmount());
            return drained;
        }
        @Override protected void onContentsChanged() { changed(); }
    };
    private final LazyOptional<IFluidHandler> handler = LazyOptional.of(() -> tank);
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        @Override protected void onContentsChanged(int slot) { changed(); }
    };

    public TankControllerBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.TANK_CONTROLLER_BE.get(), pos, state); }
    public FluidTank getTank() { updateStructure(); return tank; }
    public ItemStackHandler getInventory() { return inventory; }
    public Map<String, Long> getCountsIn() { return Map.copyOf(countsIn); }
    public Map<String, Long> getCountsOut() { return Map.copyOf(countsOut); }
    private void recordCount(Map<String, Long> counts, FluidStack fluid, int amount) {
        counts.merge(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid.getFluid()).toString(), (long) amount, Long::sum);
        changed();
    }
    private static CompoundTag saveCounts(Map<String, Long> counts) {
        CompoundTag tag = new CompoundTag(); counts.forEach(tag::putLong); return tag;
    }
    private static void loadCounts(CompoundTag tag, Map<String, Long> counts) {
        counts.clear(); for (String key : tag.getAllKeys()) counts.put(key, tag.getLong(key));
    }
    public Set<BlockPos> getMembers() { return Collections.unmodifiableSet(members); }
    public Output getOutput(int slot) { return slot >= 0 && slot < outputs.length ? outputs[slot] : null; }
    public boolean isReady() { return capacity > 0 && (level == null || level.isClientSide || structureLoaded); }
    private void changed() { setChanged(); pendingSync = true; }

    public void recordOutput(int amount) {
        if (amount > 0 && level != null && !level.isClientSide) lastOutputTick = level.getGameTime();
    }
    public void recordOutput(BlockPos pos, Direction side, int amount) {
        recordOutput(amount);
        Output output = outputAt(pos, side);
        if (output != null && amount > 0) { output.thisTick += amount; changed(); }
    }
    public boolean isOutputFlowing() {
        return level != null && lastOutputTick != Long.MIN_VALUE && level.getGameTime() - lastOutputTick <= 20;
    }
    public Output outputAt(BlockPos pos, Direction side) {
        for (Output output : outputs) if (output != null && output.pos.equals(pos) && output.side == side) return output;
        return null;
    }
    public boolean canOutput(BlockPos pos, Direction side) {
        Output output = outputAt(pos, side);
        return isReady() && output != null && output.mode.allows(level.hasNeighborSignal(pos));
    }
    public void cycleOutputMode(int slot) {
        Output output = getOutput(slot);
        if (output != null) { output.mode = output.mode.next(); changed(); }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankControllerBlockEntity entity) {
        if (entity.ticks++ % 20 == 0 || !entity.structureLoaded) {
            entity.updateStructure();
            entity.rescanOutputs();
        }
        if (entity.isReady()) entity.processContainer();
        for (Output output : entity.outputs) if (output != null) {
            output.samples[output.sampleIndex] = output.thisTick;
            output.sampleIndex = (output.sampleIndex + 1) % output.samples.length;
            output.sampleCount = Math.min(output.samples.length, output.sampleCount + 1);
            long total = 0;
            for (long sample : output.samples) total += sample;
            float average = total / (float) output.sampleCount;
            if (output.average != average) entity.pendingSync = true;
            output.average = average;
            output.thisTick = 0;
        }
        if (entity.pendingSync) {
            entity.pendingSync = false;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void updateStructure() {
        if (level == null || level.isClientSide || lastStructureTick == level.getGameTime()) return;
        lastStructureTick = level.getGameTime();
        Set<BlockPos> found = TankStructure.collect(this, members);
        if (found == null) { structureLoaded = false; return; }
        structureLoaded = true;
        if (!found.equals(members)) { members = found; changed(); }
        long blocks = members.stream().filter(p -> level.getBlockEntity(p) instanceof TankModuleBlockEntity module
                && TankStructure.kind(module.moduleName()).equals("tank")).count();
        int newCapacity = (int) Math.min(Integer.MAX_VALUE, blocks * PressureConfig.TANK_CAPACITY_PER_BLOCK.get());
        if (capacity != newCapacity) {
            capacity = newCapacity;
            tank.setCapacity(capacity);
            if (tank.getFluidAmount() > capacity) {
                FluidStack fluid = tank.getFluid().copy();
                fluid.setAmount(capacity);
                tank.setFluid(fluid);
            }
            changed();
        }
    }

    private void rescanOutputs() {
        if (!structureLoaded) return;
        Set<Face> found = new LinkedHashSet<>();
        for (BlockPos pos : members) {
            if (!(level.getBlockEntity(pos) instanceof TankModuleBlockEntity module)
                    || !TankStructure.kind(module.moduleName()).equals("output")) continue;
            for (Direction side : Direction.values()) {
                if (members.contains(pos.relative(side)) || worldPosition.equals(pos.relative(side))) continue;
                boolean connected = module.isOutputConnected(side);
                if (connected) found.add(new Face(pos, side));
            }
        }
        for (int i = 0; i < outputs.length; i++) if (outputs[i] != null
                && !found.contains(new Face(outputs[i].pos, outputs[i].side))) { outputs[i] = null; changed(); }
        for (Face face : found) if (outputAt(face.pos, face.side) == null) {
            for (int i = 0; i < outputs.length; i++) if (outputs[i] == null) {
                outputs[i] = new Output(face.pos, face.side); changed(); break;
            }
        }
    }

    /** Commit the tank change only when the resulting container fits in the output slot. */
    private void processContainer() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return;
        ItemStack copy = input.copyWithCount(1);
        var container = copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (container == null) return;
        boolean filling = !tank.isEmpty() && container.fill(tank.getFluid().copy(), FluidAction.SIMULATE) > 0;
        FluidStack moved;
        if (filling) {
            moved = tank.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            moved.setAmount(container.fill(moved, FluidAction.EXECUTE));
        } else {
            moved = container.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            int amount = tank.fill(moved, FluidAction.SIMULATE);
            if (amount <= 0) return;
            moved = container.drain(amount, FluidAction.EXECUTE);
        }
        if (moved.isEmpty()) return;
        ItemStack result = container.getContainer(), out = inventory.getStackInSlot(1);
        if (!result.isEmpty() && !out.isEmpty() && (!ItemHandlerHelper.canItemStacksStack(out, result)
                || out.getCount() + result.getCount() > Math.min(out.getMaxStackSize(), inventory.getSlotLimit(1)))) return;
        if (filling) tank.drain(moved, FluidAction.EXECUTE); else tank.fill(moved, FluidAction.EXECUTE);
        if (out.isEmpty()) inventory.setStackInSlot(1, result);
        else if (!result.isEmpty()) { ItemStack merged = out.copy(); merged.grow(result.getCount()); inventory.setStackInSlot(1, merged); }
        inventory.extractItem(0, 1, false);
    }

    public void disconnect() {
        for (BlockPos pos : members) if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof TankModuleBlockEntity module
                && worldPosition.equals(module.getControllerPos())) module.setControllerPos(null);
        members.clear();
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) { updateStructure(); return handler.cast(); }
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); handler.invalidate(); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tank.writeToNBT(tag);
        tag.putInt("capacity", capacity);
        tag.put("countsIn", saveCounts(countsIn)); tag.put("countsOut", saveCounts(countsOut));
        tag.putLong("lastOutputTick", lastOutputTick);
        tag.put("inventory", inventory.serializeNBT());
        tag.putLongArray("members", members.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag list = new ListTag();
        for (int i = 0; i < outputs.length; i++) if (outputs[i] != null) {
            Output output = outputs[i]; CompoundTag entry = new CompoundTag();
            entry.putInt("slot", i); entry.putLong("pos", output.pos.asLong());
            entry.putInt("side", output.side.ordinal()); entry.putString("mode", output.mode.name());
            entry.putFloat("average", output.average); list.add(entry);
            entry.putLongArray("samples", output.samples); entry.putInt("sampleIndex", output.sampleIndex);
            entry.putInt("sampleCount", output.sampleCount);
        }
        tag.put("outputs", list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        capacity = tag.getInt("capacity"); tank.setCapacity(capacity); tank.readFromNBT(tag);
        loadCounts(tag.getCompound("countsIn"), countsIn); loadCounts(tag.getCompound("countsOut"), countsOut);
        lastOutputTick = tag.contains("lastOutputTick") ? tag.getLong("lastOutputTick") : Long.MIN_VALUE;
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        members.clear(); for (long pos : tag.getLongArray("members")) members.add(BlockPos.of(pos));
        Arrays.fill(outputs, null);
        for (Tag value : tag.getList("outputs", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            int i = entry.getInt("slot"); if (i < 0 || i >= outputs.length) continue;
            Output output = new Output(BlockPos.of(entry.getLong("pos")), Direction.from3DDataValue(entry.getInt("side")));
            try { output.mode = RouterRedstoneMode.valueOf(entry.getString("mode")); } catch (IllegalArgumentException ignored) {}
            output.average = entry.getFloat("average"); outputs[i] = output;
            long[] samples = entry.getLongArray("samples");
            System.arraycopy(samples, 0, output.samples, 0, Math.min(samples.length, output.samples.length));
            output.sampleIndex = Math.floorMod(entry.getInt("sampleIndex"), output.samples.length);
            output.sampleCount = Math.max(0, Math.min(output.samples.length, entry.getInt("sampleCount")));
        }
    }
    private record Face(BlockPos pos, Direction side) {}
    public static final class Output {
        public final BlockPos pos;
        public final Direction side;
        public RouterRedstoneMode mode = RouterRedstoneMode.ALWAYS;
        public float average;
        private long thisTick;
        private final long[] samples = new long[20];
        private int sampleIndex, sampleCount;
        public Output(BlockPos pos, Direction side) { this.pos = pos; this.side = side; }
    }
}
