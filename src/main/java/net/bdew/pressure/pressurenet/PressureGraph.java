package net.bdew.pressure.pressurenet;

import net.bdew.pressure.api.IPressureConnectableBlock;
import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.api.IPressureHelper;
import net.bdew.pressure.api.IPressureInject;
import net.bdew.pressure.api.PressureAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PressureGraph implements IPressureHelper {
    private static final PressureGraph INSTANCE = new PressureGraph();
    private static final java.util.Map<Level, Long> CHUNK_REVISIONS = new java.util.WeakHashMap<>();

    private PressureGraph() {}

    public static void init() {
        PressureAPI.HELPER = INSTANCE;
    }

    public static PressureGraph instance() { return INSTANCE; }

    public static synchronized long chunkRevision(Level level) { return CHUNK_REVISIONS.getOrDefault(level, 0L); }

    public static synchronized void onChunkEvent(net.minecraftforge.event.level.ChunkEvent event) {
        if ((event instanceof net.minecraftforge.event.level.ChunkEvent.Load || event instanceof net.minecraftforge.event.level.ChunkEvent.Unload)
                && event.getLevel() instanceof Level level && !level.isClientSide)
            CHUNK_REVISIONS.put(level, chunkRevision(level) + 1);
    }

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide)
            invalidateNear(level, event.getPos());
    }

    public static void invalidateNear(Level level, BlockPos changedPos) {
        ArrayDeque<Visit> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(new Visit(changedPos, null));
        for (Direction direction : Direction.values()) queue.add(new Visit(changedPos.relative(direction), null));

        while (!queue.isEmpty()) {
            Visit visit = queue.removeFirst();
            if (!seen.add(visit.pos)) continue;
            if (!level.hasChunkAt(visit.pos)) continue;
            BlockState state = level.getBlockState(visit.pos);
            if (!(state.getBlock() instanceof IPressureConnectableBlock connectable)) continue;
            if (visit.from != null && !canConnect(level, visit.pos, visit.from)) continue;
            var blockEntity = level.getBlockEntity(visit.pos);
            if (blockEntity instanceof IPressureInject injector) injector.invalidateConnection(null);
            if (connectable.isTraversable(state, level, visit.pos)) {
                for (Direction direction : Direction.values()) {
                    if (canConnect(level, visit.pos, direction))
                        queue.addLast(new Visit(visit.pos.relative(direction), direction.getOpposite()));
                }
            }
        }
    }

    public static boolean canConnect(BlockGetter world, BlockPos pos, Direction side) {
        if (world instanceof Level level && (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.relative(side)))) return false;
        BlockState here = world.getBlockState(pos);
        BlockPos neighborPos = pos.relative(side);
        BlockState there = world.getBlockState(neighborPos);
        return here.getBlock() instanceof IPressureConnectableBlock a
                && there.getBlock() instanceof IPressureConnectableBlock b
                && a.canConnectTo(here, world, pos, side)
                && b.canConnectTo(there, world, neighborPos, side.getOpposite());
    }

    @Override
    public boolean canPipeConnectTo(BlockGetter world, BlockPos pos, Direction side) {
        if (world instanceof Level level && !level.hasChunkAt(pos)) return false;
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof IPressureConnectableBlock connectable
                && connectable.canConnectTo(state, world, pos, side);
    }

    @Override
    public List<Direction> getPipeConnections(BlockGetter world, BlockPos pos) {
        return connectionsAt(world, pos);
    }

    public static List<Direction> connectionsAt(BlockGetter world, BlockPos pos) {
        List<Direction> result = new ArrayList<>(6);
        for (Direction direction : Direction.values())
            if (canConnect(world, pos, direction)) result.add(direction);
        return result;
    }

    @Override
    public PressureConnection recalculateConnectionInfo(IPressureInject node, Direction side) {
        Level level = node.pressureNodeWorld();
        if (level == null || level.isClientSide) return new PressureConnection(List.of());

        BlockPos start = node.pressureNodePos().relative(side);
        ArrayDeque<Visit> queue = new ArrayDeque<>();
        queue.add(new Visit(start, side.getOpposite()));
        Set<BlockPos> seen = new HashSet<>();
        Set<Visit> endpointFaces = new HashSet<>();
        List<PressureConnection.Output> outputs = new ArrayList<>();

        while (!queue.isEmpty()) {
            Visit visit = queue.removeFirst();
            if (!level.hasChunkAt(visit.pos)) continue;
            BlockState state = level.getBlockState(visit.pos);
            if (!(state.getBlock() instanceof IPressureConnectableBlock connectable)
                    || !canConnect(level, visit.pos, visit.from)) continue;
            boolean traversable = connectable.isTraversable(state, level, visit.pos);
            if (traversable ? !seen.add(visit.pos) : !endpointFaces.add(visit)) continue;

            var blockEntity = level.getBlockEntity(visit.pos);
            if (blockEntity instanceof IPressureEject eject)
                outputs.add(new PressureConnection.Output(eject, visit.from));

            if (traversable) {
                for (Direction direction : Direction.values()) {
                    // from is the face we entered through, not the forward direction.
                    if (direction == visit.from) continue;
                    if (canConnect(level, visit.pos, direction))
                        queue.addLast(new Visit(visit.pos.relative(direction), direction.getOpposite()));
                }
            }
        }
        return new PressureConnection(outputs, node, side);
    }

    /** Complete legacy debugger scan, including every distinct endpoint face. */
    public static NetworkScan scanConnectedBlocks(Level level, BlockPos start, Direction clickedFace) {
        Set<BlockPos> pipes = new java.util.LinkedHashSet<>();
        Set<Visit> endpoints = new HashSet<>();
        List<NodeFace> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        ArrayDeque<Visit> queue = new ArrayDeque<>();
        queue.add(new Visit(start, clickedFace));
        if (clickedFace != null && canConnect(level, start, clickedFace))
            queue.add(new Visit(start.relative(clickedFace), clickedFace.getOpposite()));
        while (!queue.isEmpty()) {
            Visit visit = queue.removeFirst();
            if (!level.hasChunkAt(visit.pos)) continue;
            BlockState state = level.getBlockState(visit.pos);
            if (!(state.getBlock() instanceof IPressureConnectableBlock block)) continue;
            if (visit.from != null && !block.canConnectTo(state, level, visit.pos, visit.from)) continue;
            boolean traversable = block.isTraversable(state, level, visit.pos);
            if (traversable ? !pipes.add(visit.pos) : !endpoints.add(visit)) continue;
            var entity = level.getBlockEntity(visit.pos);
            if (entity instanceof IPressureInject) inputs.add(new NodeFace(visit.pos, visit.from));
            if (entity instanceof IPressureEject) outputs.add(new NodeFace(visit.pos, visit.from));
            if (traversable) for (Direction side : Direction.values())
                if (canConnect(level, visit.pos, side)) queue.addLast(new Visit(visit.pos.relative(side), side.getOpposite()));
        }
        return new NetworkScan(List.copyOf(inputs), List.copyOf(outputs), java.util.Collections.unmodifiableSet(pipes));
    }

    public record NodeFace(BlockPos pos, Direction side) {
        @Override public String toString() { return pos.toShortString() + "/" + (side == null ? "none" : side.getName()); }
    }
    public record NetworkScan(List<NodeFace> inputs, List<NodeFace> outputs, Set<BlockPos> traversed) {}

    private record Visit(BlockPos pos, Direction from) {}
}
