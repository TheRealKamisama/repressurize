package net.bdew.pressure.api;

import net.minecraft.core.Direction;

public interface IPressureInject extends IPressureNode {
    void invalidateConnection(Direction direction);
}
