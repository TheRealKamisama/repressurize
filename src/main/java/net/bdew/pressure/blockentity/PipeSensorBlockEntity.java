package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureConnection;
import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.api.IPressureInject;
import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.blocks.PipeSensorBlock;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public final class PipeSensorBlockEntity extends PressureNodeBlockEntity implements IPressureInject, IPressureEject {
    private final ArrayDeque<Map<String, Long>> samples = new ArrayDeque<>();
    private Map<String, Long> current = new HashMap<>();
    private final Map<String, Long> counts = new HashMap<>();
    private IPressureConnection connection;
    private int flowTicks = 100;
    private int tickCounter;

    public PipeSensorBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.PIPE_SENSOR_BE.get(), pos, state); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PipeSensorBlockEntity entity) {
        entity.flowTicks = Math.min(100, entity.flowTicks + 1);
        if (++entity.tickCounter >= 10) {
            entity.tickCounter = 0;
            boolean signal = entity.flowTicks < 10;
            if (state.getValue(PipeSensorBlock.SIGNAL) != signal)
                level.setBlock(pos, state.setValue(PipeSensorBlock.SIGNAL, signal), 3);
        }
        entity.samples.addLast(entity.current);
        entity.current = new HashMap<>();
        while (entity.samples.size() > 50) entity.samples.removeFirst();
    }

    @Override
    public int eject(FluidStack resource, Direction sideTowardNetwork, boolean doEject) {
        if (!isFluidAllowed(resource) || getLevel() == null || getLevel().isClientSide) return 0;
        Direction facing = getBlockState().getValue(PipeSensorBlock.FACING);
        if (sideTowardNetwork != facing.getOpposite()) return 0;
        if (connection == null && PressureAPI.HELPER != null)
            connection = PressureAPI.HELPER.recalculateConnectionInfo(this, facing);
        int moved = connection == null ? 0 : connection.pushFluid(resource, doEject);
        if (moved > 0 && doEject) {
            flowTicks = 0;
            String fluidId = ForgeRegistries.FLUIDS.getKey(resource.getFluid()).toString();
            current.merge(fluidId, (long) moved, Long::sum);
            counts.merge(fluidId, (long) moved, Long::sum);
            if (doEject) setChanged();
        }
        return moved;
    }

    @Override public void invalidateConnection(Direction direction) { connection = null; }

    public Map<String, Long> getCounts() { return Map.copyOf(counts); }

    public Map<String, Double> getAverages() {
        Map<String, Double> totals = new HashMap<>();
        for (var sample : samples) sample.forEach((fluid, amount) -> totals.merge(fluid, (double) amount, Double::sum));
        totals.replaceAll((fluid, amount) -> amount / Math.max(1, samples.size()));
        return Map.copyOf(totals);
    }

    public String getSummary() {
        var totals = getAverages();
        if (totals.isEmpty()) return "No fluid flowed through this sensor recently.";
        StringBuilder result = new StringBuilder("Recent fluid flow (mB/tick): ");
        boolean first = true;
        for (var entry : totals.entrySet()) {
            if (!first) result.append(", ");
            first = false;
            result.append(entry.getKey()).append(" ").append(String.format(java.util.Locale.ROOT, "%.1f", entry.getValue()));
        }
        return result.toString();
    }

    private static CompoundTag saveMap(Map<String, Long> values) {
        CompoundTag tag = new CompoundTag(); values.forEach(tag::putLong); return tag;
    }
    private static Map<String, Long> loadMap(CompoundTag tag) {
        Map<String, Long> values = new HashMap<>(); for (String key : tag.getAllKeys()) values.put(key, tag.getLong(key)); return values;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putInt("flowTicks", flowTicks); tag.putInt("sampleTicks", tickCounter);
        tag.put("counts", saveMap(counts)); tag.put("current", saveMap(current));
        var list = new net.minecraft.nbt.ListTag(); samples.forEach(sample -> list.add(saveMap(sample))); tag.put("samples", list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); flowTicks = tag.contains("flowTicks") ? tag.getInt("flowTicks") : 100; tickCounter = tag.getInt("sampleTicks");
        counts.clear(); counts.putAll(loadMap(tag.getCompound("counts"))); current = loadMap(tag.getCompound("current"));
        samples.clear();
        for (var sample : tag.getList("samples", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            samples.addLast(loadMap((CompoundTag) sample)); if (samples.size() > 50) samples.removeFirst();
        }
    }
}
