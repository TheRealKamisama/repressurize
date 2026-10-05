package net.bdew.pressure.client;

import net.bdew.pressure.Pressure;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Pressure.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PressureClientSetup {
    private PressureClientSetup() {}

    @SubscribeEvent
    public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PressureContent.INPUT_BE.get(), PressureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(PressureContent.OUTPUT_BE.get(), PressureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(PressureContent.PUMP_BE.get(), PressureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(PressureContent.DRAIN_BE.get(), PressureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(PressureContent.SLUICE_BE.get(), PressureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(PressureContent.CREATIVE_SOURCE_BE.get(), PressureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(PressureContent.ROUTER_BE.get(), PressureBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(PressureContent.TANK_MODULE_BE.get(), PressureBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void models(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        var tanks = PressureContent.BLOCKS.getEntries().stream()
                .filter(entry -> entry.get() instanceof net.bdew.pressure.blocks.TankControllerBlock
                        || entry.get() instanceof net.bdew.pressure.blocks.TankModuleBlock)
                .map(net.minecraftforge.registries.RegistryObject::getId).collect(java.util.stream.Collectors.toSet());
        event.getModels().replaceAll((location, model) -> tanks.contains(new net.minecraft.resources.ResourceLocation(
                location.getNamespace(), location.getPath())) ? new TankFrameModel(model) : model);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(PressureContent.ROUTER_MENU.get(), RouterScreen::new);
            MenuScreens.register(PressureContent.TANK_MENU.get(), TankScreen::new);
            MenuScreens.register(PressureContent.FILTER_MENU.get(), FilterScreen::new);
            MenuScreens.register(PressureContent.SENSOR_MENU.get(), SensorScreen::new);
        });
    }

    @SubscribeEvent
    public static void itemDecorations(net.minecraftforge.client.event.RegisterItemDecorationsEvent event) {
        event.register(PressureContent.CANISTER.get(), (graphics, font, stack, x, y) -> {
            var handler = stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (handler == null || handler.getFluidInTank(0).isEmpty()) return false;
            var fluid = handler.getFluidInTank(0);
            int height = Math.max(1, (int) Math.min(15, 15L * fluid.getAmount() / Math.max(1, handler.getTankCapacity(0))));
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.blit(new net.minecraft.resources.ResourceLocation("pressure", "textures/gui/canister_overlay.png"),
                    x, y, 0, 0, 5, 16, 16, 16);
            FluidGauge.draw(graphics, fluid, x + 1, y + 16 - height, 3, height);
            graphics.pose().popPose();
            return true;
        });
    }
}
