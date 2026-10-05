package net.bdew.pressure.api.properties;

import net.minecraft.world.level.material.Fluid;

public interface IFilterable {
    void setFluidFilter(Fluid fluid);
    void clearFluidFilter();
}
