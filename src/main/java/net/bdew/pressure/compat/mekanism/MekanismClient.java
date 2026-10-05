package net.bdew.pressure.compat.mekanism;

import net.bdew.pressure.client.PressureBlockEntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="pressure",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MekanismClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        if(ModList.get().isLoaded("mekanism")){
            event.registerBlockEntityRenderer(MekanismIntegration.MACHINE_BE.get(),PressureBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(MekanismIntegration.TANK_BE.get(),PressureBlockEntityRenderer::new);
        }
    }
}
