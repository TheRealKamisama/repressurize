package net.bdew.pressure.compat.mekanism;

import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.blocks.TankModuleBlock;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegistryObject;
import java.util.function.Supplier;

/** Registered only when Mekanism is present, retaining the four legacy registry IDs. */
public final class MekanismIntegration {
    public static final RegistryObject<Block> GAS_INPUT=block("gas_input",()->new MachineBlock(true));
    public static final RegistryObject<Block> GAS_OUTPUT=block("gas_output",()->new MachineBlock(false));
    public static final RegistryObject<Block> TANK_GAS_INPUT=block("tank_gas_input",TankBlock::new);
    public static final RegistryObject<Block> TANK_GAS_OUTPUT=block("tank_gas_output",TankBlock::new);
    public static final RegistryObject<BlockEntityType<GasMachineBlockEntity>> MACHINE_BE=PressureContent.BLOCK_ENTITIES.register("gas_machine",
            ()->BlockEntityType.Builder.of(GasMachineBlockEntity::new,GAS_INPUT.get(),GAS_OUTPUT.get()).build(null));
    public static final RegistryObject<BlockEntityType<GasTankBlockEntity>> TANK_BE=PressureContent.BLOCK_ENTITIES.register("tank_gas",
            ()->BlockEntityType.Builder.of(GasTankBlockEntity::new,TANK_GAS_INPUT.get(),TANK_GAS_OUTPUT.get()).build(null));
    private static RegistryObject<Block> block(String name,Supplier<Block> factory){
        var block=PressureContent.BLOCKS.register(name,factory);
        PressureContent.ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));return block;
    }
    public static void register(){
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.OnDatapackSyncEvent event)->GasConversions.clear());
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event)->GasConversions.clear());
    }
    private static final class MachineBlock extends DirectionalMachineBlock implements net.bdew.pressure.blocks.ReversibleFlowPlacement {
        private static final BooleanProperty SIGNAL=BooleanProperty.create("signal");
        private final boolean input;
        private MachineBlock(boolean input){this.input=input;registerDefaultState(defaultBlockState().setValue(SIGNAL,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(SIGNAL);}
        @Override public boolean canConnectTo(BlockState state,BlockGetter level,BlockPos pos,Direction side){return side==(input?state.getValue(FACING):state.getValue(FACING).getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new GasMachineBlockEntity(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
            return level.isClientSide?null:createTickerHelper(type,MACHINE_BE.get(),GasMachineBlockEntity::serverTick);
        }
        @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
            super.neighborChanged(state,level,pos,block,from,moving);boolean signal=level.hasNeighborSignal(pos);
            if(state.getValue(SIGNAL)!=signal)level.setBlock(pos,state.setValue(SIGNAL,signal),3);
        }
        @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
            super.onPlace(state,level,pos,old,moving);neighborChanged(state,level,pos,state.getBlock(),pos,moving);
        }
    }
    private static final class TankBlock extends TankModuleBlock {
        private TankBlock(){super(false);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new GasTankBlockEntity(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
            return level.isClientSide?null:createTickerHelper(type,TANK_BE.get(),GasTankBlockEntity::serverTick);
        }
    }
}
