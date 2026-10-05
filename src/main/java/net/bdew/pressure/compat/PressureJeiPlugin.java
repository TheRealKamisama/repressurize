package net.bdew.pressure.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;

/** Restores the legacy option to hide filled canister variants from JEI's ingredient list. */
@JeiPlugin
public final class PressureJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("pressure", "jei"); }
    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(PressureContent.CANISTER.get(),
                (mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<ItemStack>) (stack, context) ->
                        FluidUtil.getFluidContained(stack).map(fluid -> net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid.getFluid()) + ":" + fluid.getTag()).orElse("empty"));
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!PressureConfig.SHOW_CANISTERS.get()) {
            var manager=runtime.getIngredientManager();
            var filled=manager.getAllItemStacks().stream().filter(stack -> stack.is(PressureContent.CANISTER.get())
                    && FluidUtil.getFluidContained(stack).isPresent()).toList();
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,filled);
        }
    }
}
