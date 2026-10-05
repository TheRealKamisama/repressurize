package net.bdew.pressure.qa;

import mekanism.api.Action;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.compat.mekanism.*;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class MekanismGameTests {
    @GameTest(templateNamespace="pressure",template="empty")
    public static void gasRecipesRequireInstalledAdvancedAlloy(GameTestHelper helper) {
        var alloy = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekanism","alloy_infused"));
        var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("forge","alloys/advanced"));
        helper.assertTrue(new net.minecraft.world.item.ItemStack(alloy).is(tag),"Installed advanced tag must contain infused alloy");
        var dummy = new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
            @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return net.minecraft.world.item.ItemStack.EMPTY;}
        };
        for(String name:java.util.List.of("gas_input","gas_output","tank_gas_input","tank_gas_output")) {
            var recipe=(net.minecraft.world.item.crafting.ShapedRecipe)helper.getLevel().getRecipeManager().byKey(new ResourceLocation("pressure",name)).orElseThrow();
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(dummy,3,3);
            int alloySlot=-1;
            for(int i=0;i<recipe.getIngredients().size();i++) {
                var ingredient=recipe.getIngredients().get(i);
                if(ingredient==net.minecraft.world.item.crafting.Ingredient.EMPTY)continue;
                helper.assertTrue(ingredient.getItems().length>0,"Gas recipe cannot have an unresolved ingredient");
                int slot=i/recipe.getWidth()*3+i%recipe.getWidth();
                if(ingredient.test(new net.minecraft.world.item.ItemStack(alloy))) {alloySlot=slot;grid.setItem(slot,new net.minecraft.world.item.ItemStack(alloy));}
                else grid.setItem(slot,ingredient.getItems()[0].copy());
            }
            helper.assertTrue(alloySlot>=0 && recipe.matches(grid,helper.getLevel()),"Actual infused alloy crafts "+name);
            grid.setItem(alloySlot,net.minecraft.world.item.ItemStack.EMPTY);
            helper.assertTrue(!recipe.matches(grid,helper.getLevel()),"Missing alloy must reject "+name);
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(
                    java.util.UUID.nameUUIDFromBytes(("pressure-craft-"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"PressureCraft"));
            player.getAbilities().instabuild=false;player.getInventory().clearContent();
            var pos=helper.absolutePos(new BlockPos(2,1,2));helper.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState());
            var menu=new net.minecraft.world.inventory.CraftingMenu(1,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(helper.getLevel(),pos));
            for(int slot=0;slot<9;slot++)menu.slots.get(slot+1).set(grid.getItem(slot).copy());
            menu.slots.get(alloySlot+1).set(new net.minecraft.world.item.ItemStack(alloy));
            menu.slotsChanged(menu.getSlot(1).container);
            helper.assertTrue(menu.getSlot(0).getItem().is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("pressure",name))),"Survival crafting menu resolves "+name);
            menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            helper.assertTrue(menu.getCarried().is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("pressure",name))) && menu.getSlot(alloySlot+1).getItem().isEmpty(),"Taking result consumes alloy for "+name);
        }
        helper.succeed();
    }
    @GameTest(templateNamespace="pressure",template="empty",timeoutTicks=80)
    public static void gasPressureInputAndOutputConserveFluid(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1,1,1),MekanismIntegration.GAS_INPUT.get().defaultBlockState().setValue(net.bdew.pressure.blocks.DirectionalMachineBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(2,1,1),PressureContent.PIPE.get());
        helper.setBlock(new BlockPos(3,1,1),MekanismIntegration.GAS_OUTPUT.get().defaultBlockState().setValue(net.bdew.pressure.blocks.DirectionalMachineBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(4,1,1),MekanismIntegration.TANK_GAS_INPUT.get());
        helper.setBlock(new BlockPos(4,1,2),PressureContent.TANK_CONTROLLER.get());
        helper.setBlock(new BlockPos(5,1,2),PressureContent.TANK_BLOCK.get());
        helper.runAfterDelay(25,()->{
            var hydrogen=Gas.getFromRegistry(new ResourceLocation("mekanism","hydrogen"));
            var conversion=GasConversions.gas(helper.getLevel(),hydrogen);
            var input=helper.getBlockEntity(new BlockPos(1,1,1)).getCapability(GasConversions.GAS,Direction.WEST).orElseThrow(IllegalStateException::new);
            var core=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(4,1,2));
            var gas=new GasStack(hydrogen,1000*conversion.gasUnit());
            helper.assertTrue(input.insertChemical(gas,Action.SIMULATE).isEmpty(),"Gas-pressure round trip simulation must accept");
            helper.assertTrue(core.getTank().isEmpty(),"Gas-pressure simulation must not fill target");
            helper.assertTrue(input.insertChemical(gas,Action.EXECUTE).isEmpty(),"Gas-pressure output must deliver to gas intake");
            helper.assertTrue(core.getTank().getFluidAmount()==1000*conversion.fluidUnit(),"Gas-pressure path must preserve quantity");
            helper.getBlockEntity(new BlockPos(1,1,1)).getCapability(net.bdew.pressure.api.PressureAPI.FILTERABLE).ifPresent(f->f.setFluidFilter(net.minecraft.world.level.material.Fluids.LAVA));
            helper.assertTrue(input.insertChemical(gas,Action.EXECUTE).getAmount()==gas.getAmount(),"Gas input filter must reject an unrelated fluid");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace="pressure",template="empty",timeoutTicks=90)
    public static void gasTankOutputExtractsAndRespectsRedstone(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1,1,4),PressureContent.TANK_CONTROLLER.get());
        helper.setBlock(new BlockPos(2,1,4),PressureContent.TANK_BLOCK.get());
        helper.setBlock(new BlockPos(1,1,3),MekanismIntegration.TANK_GAS_INPUT.get());
        var target=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(1,1,4));target.getTank();
        helper.setBlock(new BlockPos(1,1,1),PressureContent.TANK_CONTROLLER.get());
        helper.setBlock(new BlockPos(2,1,1),PressureContent.TANK_BLOCK.get());
        helper.setBlock(new BlockPos(1,1,2),MekanismIntegration.TANK_GAS_OUTPUT.get());
        var source=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(1,1,1));source.getTank();
        helper.runAfterDelay(25,()->{
            var hydrogen=Gas.getFromRegistry(new ResourceLocation("mekanism","hydrogen"));
            var conversion=GasConversions.gas(helper.getLevel(),hydrogen);
            source.getTank().fill(conversion.fluid(1000*conversion.fluidUnit()),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            var output=helper.getBlockEntity(new BlockPos(1,1,2)).getCapability(GasConversions.GAS,Direction.SOUTH).orElseThrow(IllegalStateException::new);
            helper.assertTrue(output.extractChemical(500*conversion.gasUnit(),Action.SIMULATE).getAmount()==500*conversion.gasUnit(),"Gas extraction simulation must expose stored fluid");
            helper.assertTrue(source.getTank().getFluidAmount()==1000*conversion.fluidUnit(),"Gas simulation must not drain tank");
            source.cycleOutputMode(0);
            helper.assertTrue(output.extractChemical(500,Action.EXECUTE).isEmpty(),"Unpowered RS_ON output must not drain");
            helper.setBlock(new BlockPos(2,1,2),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
            helper.assertTrue(output.extractChemical(400*conversion.gasUnit(),Action.EXECUTE).getAmount()==400*conversion.gasUnit(),"Powered gas output must drain requested amount");
        });
        helper.runAfterDelay(30,()->{
            var conversion=GasConversions.gas(helper.getLevel(),Gas.getFromRegistry(new ResourceLocation("mekanism","hydrogen")));
            helper.assertTrue(source.getTank().isEmpty(),"Gas output must automatically deliver remaining contents");
            helper.assertTrue(target.getTank().getFluidAmount()==600*conversion.fluidUnit(),"Gas target receives only the remaining amount");
            helper.succeed();
        });
    }
    @GameTest(templateNamespace="pressure",template="empty",timeoutTicks=80)
    public static void mekanismGasUsesReversibleFluidMappingAndSimulation(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1,1,1),PressureContent.TANK_CONTROLLER.get());
        helper.setBlock(new BlockPos(2,1,1),PressureContent.TANK_BLOCK.get());
        helper.setBlock(new BlockPos(1,1,2),MekanismIntegration.TANK_GAS_INPUT.get());
        helper.runAfterDelay(25,()->{
            Gas hydrogen=Gas.getFromRegistry(new ResourceLocation("mekanism","hydrogen"));
            var conversion=GasConversions.gas(helper.getLevel(),hydrogen);
            helper.assertTrue(conversion!=null,"Mekanism hydrogen must have a reversible fluid representation");
            var input=helper.getBlockEntity(new BlockPos(1,1,2)).getCapability(GasConversions.GAS,Direction.SOUTH).orElseThrow(IllegalStateException::new);
            var core=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(1,1,1));
            GasStack offered=new GasStack(hydrogen,1000*conversion.gasUnit());
            helper.assertTrue(input.insertChemical(offered,Action.SIMULATE).isEmpty(),"Gas input simulation must report available capacity");
            helper.assertTrue(core.getTank().isEmpty(),"Gas input simulation must not mutate storage");
            helper.assertTrue(input.insertChemical(offered,Action.EXECUTE).isEmpty(),"Gas input must accept hydrogen");
            helper.assertTrue(core.getTank().getFluid().isFluidEqual(conversion.fluid(1)),"Stored fluid must represent the gas");
            helper.assertTrue(core.getTank().getFluidAmount()==1000*conversion.fluidUnit(),"Gas conversion ratio must conserve units");
            var saved=core.saveWithoutMetadata();core.load(saved);
            helper.assertTrue(core.getTank().getFluidAmount()==1000*conversion.fluidUnit(),"Converted contents must survive NBT reload");
            helper.succeed();
        });
    }
}
