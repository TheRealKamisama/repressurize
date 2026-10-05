package net.bdew.pressure.api;

import net.minecraftforge.fluids.FluidStack;

public interface IPressureConnection {
    int pushFluid(FluidStack fluid, boolean doPush);
}
