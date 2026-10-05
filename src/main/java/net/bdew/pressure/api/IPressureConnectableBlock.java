package net.bdew.pressure.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Implement this on blocks that can connect to Pressure Pipes. */
public interface IPressureConnectableBlock {
    boolean canConnectTo(BlockState state, BlockGetter world, BlockPos pos, Direction side);

    /** Return true for pipe blocks that allow a network scan to pass through them. */
    default boolean isTraversable(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }
}
