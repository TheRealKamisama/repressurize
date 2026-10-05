package net.bdew.pressure.pressurenet;

import net.bdew.pressure.api.IPressureConnection;
import net.bdew.pressure.api.IPressureEject;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public final class PressureConnection implements IPressureConnection {
    private static final ThreadLocal<Set<PressureConnection>> ACTIVE = ThreadLocal.withInitial(HashSet::new);
    private List<Output> outputs;
    private final net.bdew.pressure.api.IPressureInject source;
    private final Direction sourceSide;
    private long chunkRevision;

    public PressureConnection(List<Output> outputs) {
        this(outputs, null, null);
    }

    public PressureConnection(List<Output> outputs, net.bdew.pressure.api.IPressureInject source, Direction sourceSide) {
        this.outputs = List.copyOf(outputs);
        this.source = source;
        this.sourceSide = sourceSide;
        this.chunkRevision = source == null ? 0 : PressureGraph.chunkRevision(source.pressureNodeWorld());
    }

    @Override
    public int pushFluid(FluidStack fluid, boolean doPush) {
        if (fluid == null || fluid.isEmpty()) return 0;
        if (source != null) {
            var level = source.pressureNodeWorld();
            if (level == null || level.isClientSide || source instanceof net.minecraft.world.level.block.entity.BlockEntity entity && entity.isRemoved()) return 0;
            if (chunkRevision != PressureGraph.chunkRevision(level)) {
                PressureConnection refreshed = PressureGraph.instance().recalculateConnectionInfo(source, sourceSide);
                outputs = refreshed.outputs;
                chunkRevision = refreshed.chunkRevision;
            }
        }
        if (outputs.isEmpty()) return 0;
        Set<PressureConnection> active = ACTIVE.get();
        if (!active.add(this)) return 0;
        try {
            return pushFluidInner(fluid, doPush);
        } finally {
            active.remove(this);
            if (active.isEmpty()) ACTIVE.remove();
        }
    }

    private int pushFluidInner(FluidStack fluid, boolean doPush) {
        if (fluid == null || fluid.isEmpty() || outputs.isEmpty()) return 0;

        if (fluid.getAmount() < 10) {
            int remaining = fluid.getAmount();
            for (Output output : outputs) {
                int accepted = output.node.eject(withAmount(fluid, remaining), output.side, doPush);
                remaining -= Math.min(remaining, Math.max(0, accepted));
                if (remaining <= 0) return fluid.getAmount();
            }
            return fluid.getAmount() - remaining;
        }

        int[] capacity = new int[outputs.size()];
        long total = 0;
        for (int i = 0; i < outputs.size(); i++) {
            Output output = outputs.get(i);
            capacity[i] = Math.min(fluid.getAmount(), Math.max(0, output.node.eject(fluid.copy(), output.side, false)));
            total += capacity[i];
        }
        if (total <= 0) return 0;
        double scale = Math.min(1.0, fluid.getAmount() / (double) total);
        int moved = 0;
        int[] assigned = new int[outputs.size()];
        for (int i = 0; i < outputs.size(); i++) {
            int amount = Math.min(capacity[i], (int) Math.floor(capacity[i] * scale));
            if (amount <= 0) continue;
            Output output = outputs.get(i);
            assigned[i] = Math.min(amount, Math.max(0, output.node.eject(withAmount(fluid, amount), output.side, doPush)));
            moved += assigned[i];
        }
        // Integer rounding must not strand the final few millibuckets between outputs.
        for (int i = 0; i < outputs.size() && moved < fluid.getAmount(); i++) {
            int amount = Math.min(fluid.getAmount() - moved, capacity[i] - assigned[i]);
            if (amount > 0) {
                Output output = outputs.get(i);
                moved += Math.min(amount, Math.max(0, output.node.eject(withAmount(fluid, amount), output.side, doPush)));
            }
        }
        return moved;
    }

    private static FluidStack withAmount(FluidStack stack, int amount) {
        FluidStack copy = stack.copy();
        copy.setAmount(Math.max(0, amount));
        return copy;
    }

    public record Output(IPressureEject node, Direction side) {}
}
