package net.bdew.pressure.api;

import net.bdew.pressure.api.properties.IFilterable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/** Public entry point for integrations with Pressure Pipes Repressurized. */
public final class PressureAPI {
    private PressureAPI() {}

    public static volatile IPressureHelper HELPER;
    public static final Capability<IFilterable> FILTERABLE = CapabilityManager.get(new CapabilityToken<>() {});
}
