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
        modBus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() == PressureConfig.COMMON_SPEC)
                com.mojang.logging.LogUtils.getLogger().info("Automatic fluid-transfer limit: {} mB/t from {}. Legacy unrestricted value: 2147483647; saved settings are preserved.",
                        PressureConfig.FLUID_TRANSFER_PER_TICK.get(), event.getConfig().getFullPath());
        });
        if (net.minecraftforge.fml.ModList.get().isLoaded("mekanism"))
            net.bdew.pressure.compat.mekanism.MekanismIntegration.register();
        PressureContent.register(modBus);
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            if (net.minecraftforge.fml.ModList.get().isLoaded("computercraft"))
                event.enqueueWork(net.bdew.pressure.compat.computercraft.ComputerCraftIntegration::register);
        });
        MinecraftForge.EVENT_BUS.addListener(PressureGraph::onNeighborNotify);
        MinecraftForge.EVENT_BUS.addListener(PressureGraph::onChunkEvent);
        PressureGraph.init();
    }
}
