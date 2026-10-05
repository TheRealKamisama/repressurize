package net.bdew.pressure.qa;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidUtil;

@JeiPlugin
public final class JeiQaPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid(){return new ResourceLocation("pressureqa","verification");}
    @Override public void onRuntimeAvailable(IJeiRuntime value){runtime=value;}
    @Override public void onRuntimeUnavailable(){runtime=null;}
    public static void verify(){
        if(runtime==null)throw new IllegalStateException("JEI runtime was not delivered to QA plugin");
        var canisters=runtime.getIngredientManager().getAllItemStacks().stream().filter(s->s.is(PressureContent.CANISTER.get())).toList();
        long filled=canisters.stream().filter(s->FluidUtil.getFluidContained(s).isPresent()).count();
        if(canisters.isEmpty() || (!PressureConfig.SHOW_CANISTERS.get() && filled!=0))throw new IllegalStateException("JEI canister visibility mismatch");
        System.out.println("PRESSURE_JEI_QA_PASSED canisters="+canisters.size()+" filled="+filled);
    }
    public static void showGasRecipe(int index){
        var names=java.util.List.of("gas_input","gas_output","tank_gas_input","tank_gas_output");
        var type=mezz.jei.api.constants.RecipeTypes.CRAFTING;
        var recipe=runtime.getRecipeManager().createRecipeLookup(type).get()
                .filter(r->r.getId().equals(new ResourceLocation("pressure",names.get(index)))).findFirst().orElseThrow();
        var alloy=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekanism","alloy_infused"));
        if(recipe.getIngredients().stream().noneMatch(i->i.test(new net.minecraft.world.item.ItemStack(alloy))))
            throw new IllegalStateException("JEI gas recipe does not resolve the infused alloy");
        runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(type),java.util.List.of(recipe),java.util.List.of());
        System.out.println("PRESSURE_JEI_GAS_RECIPE "+recipe.getId()+" alloy=mekanism:alloy_infused");
    }
}
