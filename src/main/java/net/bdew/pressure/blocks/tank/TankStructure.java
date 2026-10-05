package net.bdew.pressure.blocks.tank;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.*;

/** Persistent ownership prevents adjacent controllers from counting or draining the same module. */
public final class TankStructure {
    private TankStructure() {}

    public static BlockPos owner(BlockGetter world, BlockPos pos) {
        var entity = world.getBlockEntity(pos);
        if (entity instanceof TankControllerBlockEntity) return pos;
        if (entity instanceof TankModuleBlockEntity module) return module.getControllerPos();
        return null;
    }

    public static String kind(String id) {
        return switch (id) {
            case "tank_block", "tank_indicator" -> "tank";
            case "tank_fluid_input", "tank_pressure_input", "tank_gas_input" -> "input";
            case "tank_fluid_output", "tank_fluid_output_auto", "tank_pressure_output", "tank_gas_output" -> "output";
            case "tank_filter" -> "filter";
            case "tank_interface" -> "interface";
            case "tank_sensor" -> "sensor";
            case "tank_data_port" -> "data";
            case "tank_fluid_access" -> "access";
            default -> "unknown";
        };
    }

    public static int limit(String kind) {
        var limit = net.bdew.pressure.config.PressureConfig.TANK_MODULE_LIMITS.get(kind);
        return limit == null ? 0 : limit.get();
    }

    /** Null means an unloaded member prevents safe capacity/ownership recomputation. */
    public static Set<BlockPos> collect(TankControllerBlockEntity controller, Set<BlockPos> previous) {
        Level level = controller.getLevel();
        if (level == null || previous.stream().anyMatch(p -> !level.hasChunkAt(p))) return null;
        Set<BlockPos> seen = new HashSet<>(), result = new LinkedHashSet<>();
        Map<String, Integer> counts = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(controller.getBlockPos());
        // Preserve existing reachable ownership before admitting new modules. Otherwise a
        // newly placed, nearer module can evict an older one when a module limit is full.
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos pos = current.relative(direction);
                if (!seen.add(pos) || !level.hasChunkAt(pos)) continue;
                if (level.getBlockEntity(pos) instanceof TankModuleBlockEntity module
                        && controller.getBlockPos().equals(module.getControllerPos())) {
                    result.add(pos); queue.addLast(pos);
                    counts.merge(kind(module.moduleName()), 1, Integer::sum);
                }
            }
        }
        seen.clear(); seen.addAll(result); seen.add(controller.getBlockPos());
        queue.add(controller.getBlockPos()); queue.addAll(result);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos pos = current.relative(direction);
                if (!seen.add(pos) || !level.hasChunkAt(pos)) continue;
                if (!(level.getBlockEntity(pos) instanceof TankModuleBlockEntity module)) continue;
                BlockPos owner = module.getControllerPos();
                if (owner != null && !owner.equals(controller.getBlockPos())
                        && (!level.hasChunkAt(owner) || level.getBlockEntity(owner) instanceof TankControllerBlockEntity)) continue;
                String kind = kind(module.moduleName());
                if (counts.getOrDefault(kind, 0) >= limit(kind)) continue;
                counts.merge(kind, 1, Integer::sum);
                result.add(pos);
                queue.addLast(pos);
            }
        }
        for (BlockPos pos : previous) {
            if (!result.contains(pos) && level.getBlockEntity(pos) instanceof TankModuleBlockEntity module
                    && controller.getBlockPos().equals(module.getControllerPos())) module.setControllerPos(null);
        }
        for (BlockPos pos : result) ((TankModuleBlockEntity) level.getBlockEntity(pos)).setControllerPos(controller.getBlockPos());
        return result;
    }
}
