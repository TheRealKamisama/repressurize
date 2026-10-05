package net.bdew.pressure.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Opt-in for machines whose facing selects distinct upstream/downstream sides. */
public interface ReversibleFlowPlacement {
    static Direction facing(BlockPlaceContext context) {
        Direction normal = context.getNearestLookingDirection().getOpposite();
        return context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? normal.getOpposite() : normal;
    }
}
