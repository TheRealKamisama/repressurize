package net.bdew.pressure.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

import java.util.List;

public interface IPressureHelper {
    boolean canPipeConnectTo(BlockGetter world, BlockPos pos, Direction side);
    List<Direction> getPipeConnections(BlockGetter world, BlockPos pos);
    IPressureConnection recalculateConnectionInfo(IPressureInject node, Direction side);
}
