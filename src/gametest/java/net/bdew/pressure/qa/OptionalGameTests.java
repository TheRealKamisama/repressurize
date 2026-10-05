package net.bdew.pressure.qa;

import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Only register optional tests when the actual dependency is installed. */
@Mod.EventBusSubscriber(modid="pressureqa",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class OptionalGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if(ModList.get().isLoaded("mekanism"))event.register(MekanismGameTests.class);
        if(ModList.get().isLoaded("enderio"))event.register(EnderIOGameTests.class);
        if(ModList.get().isLoaded("computercraft"))event.register(ComputerCraftGameTests.class);
    }
}
