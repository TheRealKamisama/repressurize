package net.bdew.pressure.api;

import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;

public interface IPressureEject extends IPressureNode {
    int eject(FluidStack resource, Direction sideTowardNetwork, boolean doEject);
}
