package net.bdew.pressure.compat.mekanism;

import mekanism.api.Action;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.recipes.RotaryRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;
import java.util.function.BiFunction;

/** Modern replacement for legacy Gas.getFluid: reversible rotary recipes define equivalent stored fluids. */
public final class GasConversions {
    public static final Capability<IGasHandler> GAS = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Map<RecipeManager,List<Conversion>> CACHE = new WeakHashMap<>();
    private GasConversions() {}
    public static void clear() { CACHE.clear(); }
    public static IGasHandler handler(Level level,BlockPos pos,Direction side) {
        if(level==null || !level.hasChunkAt(pos))return null;
        var entity=level.getBlockEntity(pos);
        return entity==null?null:entity.getCapability(GAS,side).orElse(null);
    }
    public static Conversion gas(Level level,Gas gas) {
        return mappings(level).stream().filter(c->c.gas==gas).findFirst().orElse(null);
    }
    public static Conversion fluid(Level level,FluidStack fluid) {
        return fluid.isEmpty()?null:mappings(level).stream().filter(c->c.fluid.isFluidEqual(fluid)).findFirst().orElse(null);
    }
    private static List<Conversion> mappings(Level level) {
        if(level==null)return List.of();
        return CACHE.computeIfAbsent(level.getRecipeManager(),manager->{
            List<Conversion> result=new ArrayList<>();
            for(var recipe:manager.getRecipes())if(recipe instanceof RotaryRecipe rotary && rotary.hasGasToFluid() && rotary.hasFluidToGas()) {
                for(FluidStack fluid:rotary.getFluidInput().getRepresentations()) {
                    GasStack gas=rotary.getGasOutput(fluid);
                    if(gas.isEmpty() || !rotary.getGasInput().testType(gas))continue;
                    FluidStack back=rotary.getFluidOutput(gas);
                    long fluidIn=rotary.getFluidInput().getNeededAmount(fluid),gasIn=rotary.getGasInput().getNeededAmount(gas);
                    if(!back.isFluidEqual(fluid) || fluidIn<=0 || gasIn<=0 || fluidIn>Integer.MAX_VALUE
                            || gas.getAmount()!=gasIn || back.getAmount()!=fluidIn)continue;
                    long divisor=gcd(gasIn,fluidIn);
                    result.add(new Conversion(fluid.copy(),gas.getType(),gasIn/divisor,(int)(fluidIn/divisor)));
                }
            }
            return List.copyOf(result);
        });
    }
    private static long gcd(long a,long b){while(b!=0){long c=a%b;a=b;b=c;}return a;}

    public static GasStack insert(Level level,GasStack offered,Action action,BiFunction<FluidStack,FluidAction,Integer> fill) {
        if(offered.isEmpty())return offered;
        Conversion c=gas(level,offered.getType());if(c==null)return offered;
        long units=Math.min(offered.getAmount()/c.gasUnit,Integer.MAX_VALUE/c.fluidUnit);
        if(units<=0)return offered;
        FluidStack candidate=c.fluid((int)(units*c.fluidUnit));
        int accepted=fill.apply(candidate,FluidAction.SIMULATE)/c.fluidUnit*c.fluidUnit;
        if(action.execute() && accepted>0)accepted=fill.apply(c.fluid(accepted),FluidAction.EXECUTE);
        long used=(long)(accepted/c.fluidUnit)*c.gasUnit;
        return used>=offered.getAmount()?GasStack.EMPTY:new GasStack(offered,offered.getAmount()-used);
    }
    public static int push(Level level,FluidStack offered,IGasHandler target,boolean execute) {
        if(target==null || offered.isEmpty())return 0;
        Conversion c=fluid(level,offered);if(c==null)return 0;
        long units=offered.getAmount()/c.fluidUnit;
        if(units<=0 || units>Long.MAX_VALUE/c.gasUnit)return 0;
        GasStack gas=new GasStack(c.gas,units*c.gasUnit);
        long accepted=gas.getAmount()-target.insertChemical(gas,Action.SIMULATE).getAmount();
        long whole=accepted/c.gasUnit;
        if(whole<=0)return 0;
        long moved=whole*c.gasUnit;
        if(execute)moved-=target.insertChemical(new GasStack(c.gas,moved),Action.EXECUTE).getAmount();
        return (int)(moved/c.gasUnit)*c.fluidUnit;
    }
    public record Conversion(FluidStack fluid,Gas gas,long gasUnit,int fluidUnit) {
        public FluidStack fluid(int amount){FluidStack copy=fluid.copy();copy.setAmount(amount);return copy;}
        public GasStack gas(int amount){return new GasStack(gas,(long)(amount/fluidUnit)*gasUnit);}
    }

    public abstract static class Handler implements IGasHandler {
        @Override public int getTanks(){return 1;}
        @Override public GasStack getChemicalInTank(int tank){return GasStack.EMPTY;}
        @Override public void setChemicalInTank(int tank,GasStack stack){}
        @Override public long getTankCapacity(int tank){return Integer.MAX_VALUE;}
        @Override public boolean isValid(int tank,GasStack stack){return false;}
        @Override public GasStack insertChemical(int tank,GasStack stack,Action action){return stack;}
        @Override public GasStack extractChemical(int tank,long amount,Action action){return GasStack.EMPTY;}
    }
}
