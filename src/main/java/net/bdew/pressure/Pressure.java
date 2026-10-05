package net.bdew.pressure;

import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Pressure.MOD_ID)
public final class Pressure {
    public static final String MOD_ID = "pressure";

    public Pressure() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PressureConfig.COMMON_SPEC);
        if (net.minecraftforge.fml.ModList.get().isLoaded("mekanism"))
            net.bdew.pressure.compat.mekanism.MekanismIntegration.register();
        PressureContent.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(PressureGraph::onNeighborNotify);
        MinecraftForge.EVENT_BUS.addListener(PressureGraph::onChunkEvent);
        PressureGraph.init();
    }
}
