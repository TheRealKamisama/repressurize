package net.bdew.pressure.qa;

import net.bdew.pressure.compat.EnderIOFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Supplier;

@PrefixGameTestTemplate(false)
public final class EnderIOGameTests {
    @GameTest(templateNamespace="pressure",template="empty",timeoutTicks=100)
    public static void enderIOConduitFilterLocksAndClearsGraph(GameTestHelper helper) throws Exception {
        var block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("enderio","conduit"));
        helper.assertTrue(block!=null && block!=net.minecraft.world.level.block.Blocks.AIR,"EnderIO conduit block must be registered");
        BlockPos pos=new BlockPos(1,1,1);helper.setBlock(pos,block);
        var entity=helper.getBlockEntity(pos);
        Object bundle=entity.getClass().getMethod("getBundle").invoke(entity);
        Class<?> typeClass=Class.forName("com.enderio.api.conduit.ConduitType");
        Object type=((Supplier<?>)Class.forName("com.enderio.conduits.common.init.EIOConduitTypes").getField("FLUID").get(null)).get();
        bundle.getClass().getMethod("addType",net.minecraft.world.level.Level.class,typeClass,net.minecraft.world.entity.player.Player.class)
                .invoke(bundle,helper.getLevel(),type,FakePlayerFactory.getMinecraft(helper.getLevel()));
        helper.runAfterDelay(25,()->{
            try {
                var filter=EnderIOFilter.find(entity);helper.assertTrue(filter!=null,"Filter adapter must find the normal fluid conduit");
                filter.setFluidFilter(Fluids.LAVA);
                Object node=bundle.getClass().getMethod("getNodeForTypeExact",typeClass).invoke(bundle,type);
                Object data=node.getClass().getMethod("getConduitData").invoke(node);
                helper.assertTrue(data.getClass().getMethod("lockedFluid").invoke(data)==Fluids.LAVA,"Conduit must be locked to selected fluid");
                filter.clearFluidFilter();
                helper.assertTrue(data.getClass().getMethod("lockedFluid").invoke(data)==null,"Conduit lock must clear");
                helper.succeed();
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        });
    }
}
