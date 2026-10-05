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
}
