package net.bdew.pressure.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IPressureNode {
    BlockPos pressureNodePos();
    Level pressureNodeWorld();
}
