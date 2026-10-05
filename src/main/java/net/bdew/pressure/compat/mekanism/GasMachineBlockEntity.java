package net.bdew.pressure.compat.mekanism;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.bdew.pressure.api.*;
import net.bdew.pressure.blockentity.PressureNodeBlockEntity;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public final class GasMachineBlockEntity extends PressureNodeBlockEntity implements IPressureInject, IPressureEject {
    private IPressureConnection connection;
    private final IGasHandler input=new GasConversions.Handler() {
        @Override public boolean isValid(int tank,GasStack gas){
            var conversion=GasConversions.gas(level,gas.getType());
            return isInput() && tank==0 && conversion!=null && isFluidAllowed(conversion.fluid(1));
        }
        @Override public GasStack insertChemical(int tank,GasStack stack,Action action) {
            if(level==null || level.isClientSide || !isInput() || tank!=0)return stack;
            return GasConversions.insert(level,stack,action,GasMachineBlockEntity.this::push);
        }
    };
    private final LazyOptional<IGasHandler> gasCap=LazyOptional.of(()->input);
    public GasMachineBlockEntity(BlockPos pos,BlockState state){super(MekanismIntegration.MACHINE_BE.get(),pos,state);}
    private boolean isInput(){return getBlockState().is(MekanismIntegration.GAS_INPUT.get());}
    private Direction facing(){return getBlockState().getValue(DirectionalMachineBlock.FACING);}
    private int push(FluidStack fluid,FluidAction action) {
        if(!isFluidAllowed(fluid))return 0;
        if(connection==null)connection=PressureGraph.instance().recalculateConnectionInfo(this,facing());
        return connection.pushFluid(fluid,action.execute());
    }
    @Override public void invalidateConnection(Direction side){connection=null;}
    @Override public int eject(FluidStack fluid,Direction side,boolean execute) {
        if(level==null || level.isClientSide || isInput() || side!=facing().getOpposite() || !isFluidAllowed(fluid))return 0;
        return GasConversions.push(level,fluid,GasConversions.handler(level,worldPosition.relative(facing()),facing().getOpposite()),execute);
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,GasMachineBlockEntity entity) {
        if(!entity.isInput() || !level.hasNeighborSignal(pos))return;
        IGasHandler source=GasConversions.handler(level,pos.relative(entity.facing().getOpposite()),entity.facing());
        if(source==null)return;
        for(int tank=0;tank<source.getTanks();tank++) {
            GasStack offered=source.extractChemical(tank,Integer.MAX_VALUE,Action.SIMULATE);
            if(offered.isEmpty())continue;
            GasStack remaining=entity.input.insertChemical(0,offered,Action.EXECUTE);
            long moved=offered.getAmount()-remaining.getAmount();
            if(moved>0)source.extractChemical(tank,moved,Action.EXECUTE);
        }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side) {
        if(capability==GasConversions.GAS && side==(isInput()?facing().getOpposite():facing()))return gasCap.cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();gasCap.invalidate();}
}
