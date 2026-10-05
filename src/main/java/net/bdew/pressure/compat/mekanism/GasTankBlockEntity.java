package net.bdew.pressure.compat.mekanism;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.Map;

public final class GasTankBlockEntity extends TankModuleBlockEntity {
    private final Map<Direction,LazyOptional<IGasHandler>> handlers=new EnumMap<>(Direction.class);
    public GasTankBlockEntity(BlockPos pos,BlockState state){super(MekanismIntegration.TANK_BE.get(),pos,state);}
    private boolean input(){return getBlockState().is(MekanismIntegration.TANK_GAS_INPUT.get());}
    @Override public boolean isOutputConnected(Direction side){return GasConversions.handler(level,worldPosition.relative(side),side.getOpposite())!=null;}
    private IGasHandler handler(Direction side){return new GasConversions.Handler(){
        @Override public GasStack getChemicalInTank(int tank){
            var core=tankController();if(core==null || input() || tank!=0)return GasStack.EMPTY;
            var fluid=core.getTank().getFluid();var conversion=GasConversions.fluid(level,fluid);
            return conversion==null?GasStack.EMPTY:conversion.gas(fluid.getAmount());
        }
        @Override public boolean isValid(int tank,GasStack gas){return input() && tank==0 && GasConversions.gas(level,gas.getType())!=null;}
        @Override public GasStack insertChemical(int tank,GasStack stack,Action action){
            var core=tankController();if(core==null || !input() || tank!=0)return stack;
            return GasConversions.insert(level,stack,action,(fluid,a)->isFluidAllowed(fluid)?core.getTank().fill(fluid,a):0);
        }
        @Override public GasStack extractChemical(int tank,long amount,Action action){
            var core=tankController();if(core==null || input() || tank!=0 || !core.canOutput(worldPosition,side))return GasStack.EMPTY;
            var fluid=core.getTank().getFluid();var conversion=GasConversions.fluid(level,fluid);if(conversion==null)return GasStack.EMPTY;
            long units=Math.min(amount/conversion.gasUnit(),fluid.getAmount()/conversion.fluidUnit());
            if(units<=0)return GasStack.EMPTY;
            int drained=(int)units*conversion.fluidUnit();
            if(action.execute()){core.getTank().drain(drained,FluidAction.EXECUTE);core.recordOutput(worldPosition,side,drained);}
            return conversion.gas(drained);
        }
    };}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side){
        if(capability==GasConversions.GAS)return handlers.computeIfAbsent(side==null?Direction.UP:side,d->LazyOptional.of(()->handler(d))).cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();handlers.values().forEach(LazyOptional::invalidate);handlers.clear();}
    public static void serverTick(Level level,BlockPos pos,BlockState state,GasTankBlockEntity entity){
        if(entity.input())return;
        var core=entity.tankController();if(core==null)return;
        for(Direction side:Direction.values())if(core.canOutput(pos,side)){
            var tank=core.getTank();
            int moved=GasConversions.push(level,tank.getFluid().copy(),GasConversions.handler(level,pos.relative(side),side.getOpposite()),true);
            if(moved>0){tank.drain(moved,FluidAction.EXECUTE);core.recordOutput(pos,side,moved);}
        }
    }
}
