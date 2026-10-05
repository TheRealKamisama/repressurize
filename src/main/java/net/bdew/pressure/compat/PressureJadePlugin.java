package net.bdew.pressure.compat;

import net.bdew.pressure.blockentity.*;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Server-sourced equivalent of the legacy Waila tank/module/filter/valve providers. */
@WailaPlugin
public final class PressureJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.INSTANCE,PressureNodeBlockEntity.class);
        registration.registerBlockDataProvider(Provider.INSTANCE,CheckValveBlockEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE,Block.class);
    }
    public enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return new ResourceLocation("pressure","details"); }
        @Override public void appendServerData(CompoundTag data,BlockAccessor accessor) {
            var node=accessor.getBlockEntity();
            if(!(node instanceof PressureNodeBlockEntity) && !(node instanceof CheckValveBlockEntity)) return;
            CompoundTag info=new CompoundTag();
            TankControllerBlockEntity core=node instanceof TankControllerBlockEntity c?c
                    :node instanceof TankModuleBlockEntity module?module.tankController():null;
            if(core!=null) {
                var tank=core.getTank();
                info.put("fluid",tank.getFluid().writeToNBT(new CompoundTag()));
                info.putInt("capacity",tank.getCapacity());
                for(int i=0;i<6;i++) {
                    var output=core.getOutput(i);
                    if(output!=null && output.pos.equals(accessor.getPosition()) && output.side==accessor.getSide()) {
                        info.putInt("output",i);info.putFloat("flow",output.average);
                        info.putString("redstone",output.mode.translationKey());
                    }
                }
            }
            var filter=core!=null?core.getFluidFilter():node instanceof PressureNodeBlockEntity pressure?pressure.getFluidFilter():null;
            if(node instanceof RouterBlockEntity router) {
                filter=router.getSideFilter(accessor.getSide());
                info.putString("mode",router.getSideMode(accessor.getSide()).translationKey());
                info.putString("redstone",router.getRedstoneMode(accessor.getSide()).translationKey());
            }
            if(filter!=null) info.putString("filter",ForgeRegistries.FLUIDS.getKey(filter).toString());
            if(node instanceof PipeSensorBlockEntity sensor) info.putString("sensor",sensor.getSummary());
            if(node instanceof CheckValveBlockEntity) info.putBoolean("closed",accessor.getBlockState().getValue(net.bdew.pressure.blocks.CheckValveBlock.SIGNAL));
            if(accessor.getBlockState().getBlock() instanceof DirectionalMachineBlock) {
                var facing=accessor.getBlockState().getValue(DirectionalMachineBlock.FACING);
                if(accessor.getSide()==facing) info.putString("face","pressure.waila.side.output");
                else if(accessor.getSide()==facing.getOpposite()) info.putString("face","pressure.waila.side.input");
            }
            data.put("pressure",info);
        }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config) {
            if(!accessor.getServerData().contains("pressure")) return;
            CompoundTag info=accessor.getServerData().getCompound("pressure");
            if(info.contains("capacity")) {
                FluidStack fluid=FluidStack.loadFluidStackFromNBT(info.getCompound("fluid"));
                tooltip.add(fluid.isEmpty()?Component.translatable("pressure.canister.empty"):fluid.getDisplayName());
                tooltip.add(Component.literal(fluid.getAmount()+" / "+info.getInt("capacity")+" mB"));
            }
            if(info.contains("filter")) {
                var id=ResourceLocation.tryParse(info.getString("filter"));
                if(id!=null && ForgeRegistries.FLUIDS.containsKey(id))
                    tooltip.add(Component.translatable("pressure.waila.filter",new FluidStack(ForgeRegistries.FLUIDS.getValue(id),1).getDisplayName()));
            }
            for(String key:new String[]{"mode","redstone","face"}) if(info.contains(key)) tooltip.add(Component.translatable(info.getString(key)));
            if(info.contains("output")) {
                tooltip.add(Component.translatable("pressure.output."+info.getInt("output")));
                tooltip.add(Component.literal(String.format(java.util.Locale.ROOT,"%.1f mB/t",info.getFloat("flow"))));
            }
            if(info.contains("sensor")) tooltip.add(Component.literal(info.getString("sensor")));
            if(info.contains("closed")) tooltip.add(Component.translatable(info.getBoolean("closed")?"pressure.waila.valve.closed":"pressure.waila.valve.open"));
        }
    }
}
