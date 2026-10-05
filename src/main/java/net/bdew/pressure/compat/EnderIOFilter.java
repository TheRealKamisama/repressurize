package net.bdew.pressure.compat;

import net.bdew.pressure.api.properties.IFilterable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;

/** Optional bridge for EnderIO 6.2's public conduit graph API; no EnderIO classes load in a base install. */
public final class EnderIOFilter {
    private EnderIOFilter() {}
    @Nullable public static IFilterable find(BlockEntity entity) {
        if (entity == null || !entity.getClass().getName().equals("com.enderio.conduits.common.conduit.block.ConduitBlockEntity")) return null;
        try {
            Object bundle=entity.getClass().getMethod("getBundle").invoke(entity);
            Class<?> typeClass=Class.forName("com.enderio.api.conduit.ConduitType");
            Method getNode=bundle.getClass().getMethod("getNodeForTypeExact",typeClass);
            for(Object type:(Collection<?>)bundle.getClass().getMethod("getTypes").invoke(bundle)) {
                if(!type.getClass().getName().equals("com.enderio.conduits.common.conduit.type.fluid.FluidConduitType")
                        || (boolean)type.getClass().getMethod("isMultiFluid").invoke(type)) continue;
                return new IFilterable() {
                    @Override public void setFluidFilter(Fluid fluid){apply(fluid);}
                    @Override public void clearFluidFilter(){apply(null);}
                    private void apply(@Nullable Fluid fluid) {
                        if(entity.getLevel()==null || entity.getLevel().isClientSide || entity.isRemoved()) return;
                        try {
                            Object node=getNode.invoke(bundle,type);
                            if(node==null)return;
                            Object graph=node.getClass().getMethod("getParentGraph").invoke(node);
                            Collection<?> nodes=graph==null?java.util.List.of(node):(Collection<?>)graph.getClass().getMethod("getNodes").invoke(graph);
                            for(Object member:nodes) {
                                Object data=member.getClass().getMethod("getConduitData").invoke(member);
                                data.getClass().getMethod("setShouldReset",boolean.class).invoke(data,false);
                                data.getClass().getMethod("setLockedFluid",Fluid.class).invoke(data,fluid);
                                BlockPos pos=(BlockPos)member.getClass().getMethod("getPos").invoke(member);
                                BlockEntity tile=entity.getLevel().getBlockEntity(pos);
                                if(tile!=null) {tile.setChanged();entity.getLevel().sendBlockUpdated(pos,tile.getBlockState(),tile.getBlockState(),3);}
                            }
                        } catch(ReflectiveOperationException e) {
                            com.mojang.logging.LogUtils.getLogger().error("EnderIO conduit filter API changed; could not update the network",e);
                        }
                    }
                };
            }
        } catch(ReflectiveOperationException e) {
            com.mojang.logging.LogUtils.getLogger().warn("EnderIO conduit filter API unavailable",e);
        }
        return null;
    }
}
