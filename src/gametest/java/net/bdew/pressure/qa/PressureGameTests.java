package net.bdew.pressure.qa;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.config.PressureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("pressure")
@PrefixGameTestTemplate(false)
public final class PressureGameTests {
    @GameTest(template="empty")
    public static void networkFindsRouterInputAfterEncounteringItsOutputFace(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1,1,3),PressureContent.INPUT.get().defaultBlockState().setValue(net.bdew.pressure.blocks.InputBlock.FACING,Direction.EAST));
        for(var pos:java.util.List.of(new BlockPos(2,1,3),new BlockPos(2,1,2),new BlockPos(3,1,2)))helper.setBlock(pos,PressureContent.PIPE.get());
        helper.setBlock(new BlockPos(3,1,3),PressureContent.ROUTER.get());
        var core=tank(helper,new BlockPos(4,1,3));
        var router=(net.bdew.pressure.blockentity.RouterBlockEntity)helper.getBlockEntity(new BlockPos(3,1,3));
        for(int i=0;i<5;i++){router.cycleSideMode(Direction.WEST);router.cycleSideMode(Direction.EAST);}
        router.cycleSideMode(Direction.NORTH);
        var input=helper.getBlockEntity(new BlockPos(1,1,3)).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        helper.assertTrue(input.fill(new FluidStack(Fluids.WATER,1234),FluidAction.EXECUTE)==1234,"Network must inspect a second router face after a rejecting output face");
        helper.assertTrue(core.getTank().getFluidAmount()==1234,"Multi-face route must conserve fluid");
        var scan=net.bdew.pressure.pressurenet.PressureGraph.scanConnectedBlocks(helper.getLevel(),helper.absolutePos(new BlockPos(2,1,3)),Direction.UP);
        helper.assertTrue(scan.traversed().size()==3 && !scan.inputs().isEmpty() && !scan.outputs().isEmpty(),"Debugger must enumerate the complete connected graph");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void savedFlowingFluidCanisterPlacesSource(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("pressure-flowing-canister".getBytes()),"PressureFlowing"));
        var stack=new ItemStack(PressureContent.CANISTER.get());
        stack.getOrCreateTag().putString("FluidId","minecraft:flowing_water");stack.getOrCreateTag().putInt("Amount",2500);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        BlockPos floor=helper.absolutePos(new BlockPos(2,1,2));helper.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        player.setPos(Vec3.atCenterOf(floor).add(0,2,0));
        player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
        helper.assertTrue(amount(player.getMainHandItem())==1500,"Saved flowing canister must consume exactly a bucket");
        helper.assertTrue(helper.getLevel().getFluidState(floor.above()).isSource(),"Saved flowing canister must place a source, not a transient flow");
        helper.runAfterDelay(30,()->{helper.assertTrue(helper.getLevel().getFluidState(floor.above()).isSource(),"Normalized source must persist");helper.succeed();});
    }
    @GameTest(template="empty",batch="machine_transfer")
    public static void poweredPumpPreservesLegacyThroughputAndFilter(GameTestHelper helper) {
        int previous=PressureConfig.FLUID_TRANSFER_PER_TICK.get();PressureConfig.FLUID_TRANSFER_PER_TICK.set(Integer.MAX_VALUE);
        try{
            helper.setBlock(new BlockPos(1,1,1),PressureContent.WATER_SOURCE.get());
            helper.setBlock(new BlockPos(2,1,1),PressureContent.PUMP.get().defaultBlockState().setValue(net.bdew.pressure.blocks.PumpBlock.FACING,Direction.EAST));
            var core=tank(helper,new BlockPos(3,1,1));
            var pump=(net.bdew.pressure.blockentity.PumpBlockEntity)helper.getBlockEntity(new BlockPos(2,1,1));
            net.bdew.pressure.blockentity.PumpBlockEntity.serverTick(helper.getLevel(),pump.getBlockPos(),pump.getBlockState(),pump);
            helper.assertTrue(core.getTank().isEmpty(),"Unpowered pump must not pull");
            helper.setBlock(new BlockPos(2,2,1),Blocks.REDSTONE_BLOCK);
            net.bdew.pressure.blockentity.PumpBlockEntity.serverTick(helper.getLevel(),pump.getBlockPos(),pump.getBlockState(),pump);
            helper.assertTrue(core.getTank().getFluidAmount()==core.getTank().getCapacity(),"Legacy unrestricted pump must move more than 4000 mB in a tick");
            core.getTank().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
            pump.getCapability(net.bdew.pressure.api.PressureAPI.FILTERABLE).ifPresent(f->f.setFluidFilter(Fluids.LAVA));
            net.bdew.pressure.blockentity.PumpBlockEntity.serverTick(helper.getLevel(),pump.getBlockPos(),pump.getBlockState(),pump);
            helper.assertTrue(core.getTank().isEmpty(),"Powered pump must enforce its filter");
            helper.succeed();
        }finally{PressureConfig.FLUID_TRANSFER_PER_TICK.set(previous);}
    }

    @GameTest(template="empty",batch="router_transfer")
    public static void routerPrioritiesGatesAndUnfilteredReload(GameTestHelper helper) {
        int previous=PressureConfig.FLUID_TRANSFER_PER_TICK.get();PressureConfig.FLUID_TRANSFER_PER_TICK.set(1000);
        try {
        helper.setBlock(new BlockPos(3,1,3),PressureContent.ROUTER.get());
        helper.setBlock(new BlockPos(3,1,4),PressureContent.WATER_SOURCE.get());
        helper.setBlock(new BlockPos(4,1,3),PressureContent.TANK_CONTROLLER.get());helper.setBlock(new BlockPos(5,1,3),PressureContent.TANK_BLOCK.get());
        helper.setBlock(new BlockPos(2,1,3),PressureContent.TANK_CONTROLLER.get());helper.setBlock(new BlockPos(1,1,3),PressureContent.TANK_BLOCK.get());
        helper.setBlock(new BlockPos(3,1,2),PressureContent.TANK_CONTROLLER.get());helper.setBlock(new BlockPos(3,1,1),PressureContent.TANK_BLOCK.get());
        var high=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(4,1,3));
        var middle=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(2,1,3));
        var low=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(3,1,2));
        var router=(net.bdew.pressure.blockentity.RouterBlockEntity)helper.getBlockEntity(new BlockPos(3,1,3));
        for(int i=0;i<2;i++)router.cycleSideMode(Direction.SOUTH);
        for(int i=0;i<5;i++)router.cycleSideMode(Direction.EAST);
        for(int i=0;i<4;i++)router.cycleSideMode(Direction.WEST);
        for(int i=0;i<3;i++)router.cycleSideMode(Direction.NORTH);
        Runnable tick=()->net.bdew.pressure.blockentity.RouterBlockEntity.serverTick(helper.getLevel(),router.getBlockPos(),router.getBlockState(),router);
        tick.run();helper.assertTrue(high.getTank().getFluidAmount()>0 && middle.getTank().isEmpty() && low.getTank().isEmpty(),"High priority must receive first");
        high.getTank().fill(new FluidStack(Fluids.WATER,Integer.MAX_VALUE),FluidAction.EXECUTE);
        tick.run();helper.assertTrue(middle.getTank().getFluidAmount()>0 && low.getTank().isEmpty(),"Medium priority must precede low");
        middle.getTank().fill(new FluidStack(Fluids.WATER,Integer.MAX_VALUE),FluidAction.EXECUTE);
        tick.run();helper.assertTrue(low.getTank().getFluidAmount()>0,"Low priority must receive when higher outputs are full");
        for(var core:java.util.List.of(high,middle,low))core.getTank().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
        router.cycleRedstoneMode(Direction.SOUTH);tick.run();helper.assertTrue(high.getTank().isEmpty(),"RS_ON input must wait for power");
        helper.setBlock(new BlockPos(3,2,3),Blocks.REDSTONE_BLOCK);tick.run();helper.assertTrue(high.getTank().getFluidAmount()>0,"Powered RS_ON input must pull");
        high.getTank().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);router.cycleRedstoneMode(Direction.SOUTH);tick.run();
        helper.assertTrue(high.getTank().isEmpty(),"Powered RS_OFF input must stop");
        router.cycleRedstoneMode(Direction.SOUTH);tick.run();helper.assertTrue(high.getTank().isEmpty(),"NEVER input must stop");
        router.cycleRedstoneMode(Direction.SOUTH);
        var saved=router.saveWithoutMetadata();router.load(saved);
        helper.assertTrue(router.getSideFilter(Direction.SOUTH)==null,"Unfiltered router must stay unfiltered after NBT load");
        for(int i=0;i<5;i++)router.cycleSideMode(Direction.SOUTH);
        var input=router.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.SOUTH).orElseThrow(IllegalStateException::new);
        helper.assertTrue(input.fill(new FluidStack(Fluids.WATER,1234),FluidAction.EXECUTE)==1234,"Passive router input must accept fluid");
        for(int i=0;i<5;i++)router.cycleSideMode(Direction.SOUTH);
        helper.assertTrue(input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Disabled input must reject fluid");
        helper.succeed();
        } finally { PressureConfig.FLUID_TRANSFER_PER_TICK.set(previous); }
    }

    @GameTest(template="empty")
    public static void pipeSensorCountsOnlyExecutedTransfersAndPersists(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1,1,1),PressureContent.PIPE_SENSOR.get().defaultBlockState().setValue(net.bdew.pressure.blocks.PipeSensorBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(2,1,1),PressureContent.PIPE.get());
        helper.setBlock(new BlockPos(3,1,1),PressureContent.OUTPUT.get().defaultBlockState().setValue(net.bdew.pressure.blocks.OutputBlock.FACING,Direction.EAST));
        var core=tank(helper,new BlockPos(4,1,1));
        var sensor=(net.bdew.pressure.blockentity.PipeSensorBlockEntity)helper.getBlockEntity(new BlockPos(1,1,1));
        helper.assertTrue(sensor.eject(new FluidStack(Fluids.WATER,1000),Direction.WEST,false)==1000,"Sensor simulation must preserve route capacity");
        helper.assertTrue(sensor.getCounts().isEmpty(),"Simulated flow must not be counted");
        sensor.eject(new FluidStack(Fluids.WATER,1000),Direction.WEST,true);
        net.bdew.pressure.blockentity.PipeSensorBlockEntity.serverTick(helper.getLevel(),sensor.getBlockPos(),sensor.getBlockState(),sensor);
        var saved=sensor.saveWithoutMetadata();sensor.load(saved);
        helper.assertTrue(sensor.getCounts().getOrDefault("minecraft:water",0L)==1000L,"Sensor count must persist");
        helper.assertTrue(sensor.getAverages().getOrDefault("minecraft:water",0D)==1000D,"Sensor average history must persist");
        helper.assertTrue(core.getTank().getFluidAmount()==1000,"Sensor must deliver the counted fluid");helper.succeed();
    }
    @GameTest(template="empty",batch="module_limits",timeoutTicks=80)
    public static void existingTankOwnershipSurvivesNewModuleAtLimit(GameTestHelper helper) {
        var limit=PressureConfig.TANK_MODULE_LIMITS.get("tank");int previous=limit.get();limit.set(1);
        try{
            helper.setBlock(new BlockPos(2,1,2),PressureContent.TANK_CONTROLLER.get());
            helper.setBlock(new BlockPos(2,1,3),PressureContent.TANK_BLOCK.get());
            var core=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(2,1,2));core.getTank();
            helper.setBlock(new BlockPos(2,1,1),PressureContent.TANK_BLOCK.get());
            helper.runAfterDelay(25,()->{
                try{
                    core.getTank();
                    var original=(TankModuleBlockEntity)helper.getBlockEntity(new BlockPos(2,1,3));
                    var added=(TankModuleBlockEntity)helper.getBlockEntity(new BlockPos(2,1,1));
                    helper.assertTrue(core.getBlockPos().equals(original.getControllerPos()),"New module must not evict existing owned module at capacity");
                    helper.assertTrue(added.getControllerPos()==null,"Module above configured limit must remain unassembled");
                    helper.succeed();
                }finally{limit.set(previous);}
            });
        }catch(Throwable error){limit.set(previous);throw error;}
    }
    @GameTest(template="empty")
    public static void allPressureRecipesCraftAndBlocksDrop(GameTestHelper helper) {
        var dummy=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int index){return ItemStack.EMPTY;}
        };
        int recipes=0,blocks=0;
        for(var recipe:helper.getLevel().getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if(!recipe.getId().getNamespace().equals("pressure"))continue;
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(dummy,3,3);
            int width=recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped?shaped.getWidth():3;
            var ingredients=recipe.getIngredients();
            for(int i=0;i<ingredients.size();i++) {
                var ingredient=ingredients.get(i);
                if(!ingredient.isEmpty()) {
                    helper.assertTrue(ingredient.getItems().length>0,"Recipe has unresolved ingredient: "+recipe.getId());
                    grid.setItem(i/width*3+i%width,ingredient.getItems()[0].copy());
                }
            }
            helper.assertTrue(recipe.matches(grid,helper.getLevel()),"Recipe does not match its ingredient grid: "+recipe.getId());
            helper.assertTrue(!recipe.assemble(grid,helper.getLevel().registryAccess()).isEmpty(),"Recipe crafted an empty result: "+recipe.getId());
            recipes++;
        }
        for(var entry:PressureContent.BLOCKS.getEntries()) {
            var block=entry.get();BlockPos pos=new BlockPos(1,1,1);helper.setBlock(pos,block);
            var drops=net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(),helper.getLevel(),helper.absolutePos(pos),helper.getLevel().getBlockEntity(helper.absolutePos(pos)),null,new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(drops.stream().anyMatch(stack->stack.is(block.asItem())),"Missing own-item block loot: "+entry.getId());
            blocks++;
        }
        helper.assertTrue(recipes>=33 && blocks>=24,"Recipe/content set unexpectedly shrank");
        System.out.println("PRESSURE_CONTENT_QA_PASSED recipes="+recipes+" blockDrops="+blocks);
        helper.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void canisterNetherWaterAndCreativeConsumption(GameTestHelper helper) {
        var nether=helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        helper.assertTrue(nether!=null,"Test server must have a Nether dimension");
        var player=FakePlayerFactory.get(nether,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("pressure-nether-test".getBytes()),"PressureNether"));
        BlockPos floor=new BlockPos(0,100,0);
        nether.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());nether.setBlockAndUpdate(floor.above(),Blocks.AIR.defaultBlockState());
        player.setPos(Vec3.atCenterOf(floor).add(0,2,0));player.getAbilities().instabuild=true;
        player.setItemInHand(InteractionHand.MAIN_HAND,canister(2500));
        var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
        helper.assertTrue(result.consumesAction(),"Nether water vaporization must succeed");
        helper.assertTrue(amount(player.getMainHandItem())==1500,"Nether must consume one bucket even in creative");
        helper.assertTrue(nether.getFluidState(floor.above()).isEmpty(),"Nether water must vaporize");
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void handPumpReplacesOneStackedBucket(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1,1,1),PressureContent.WATER_SOURCE.get());
        var player=FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("pressure-hand-pump-test".getBytes()),"PressurePump"));
        player.getInventory().clearContent();player.getInventory().selected=0;
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PressureContent.HAND_PUMP.get()));
        player.getInventory().setItem(1,new ItemStack(Items.BUCKET,3));
        BlockPos source=helper.absolutePos(new BlockPos(1,1,1));player.setPos(Vec3.atCenterOf(source));
        var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(source),Direction.UP,source,false)));
        helper.assertTrue(result.consumesAction(),"Hand pump should fill one bucket");
        helper.assertTrue(player.getInventory().getItem(1).getCount()==2,"Exactly one bucket leaves the stack");
        helper.assertTrue(player.getInventory().items.stream().filter(s->s.is(Items.WATER_BUCKET)).mapToInt(ItemStack::getCount).sum()==1,"Resulting filled bucket must be present");
        helper.assertTrue(player.getMainHandItem().getDamageValue()==1,"Successful survival transfer consumes one durability");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=80)
    public static void tankMenuFilterShiftClickAndOutputSettings(GameTestHelper helper) {
        BlockPos pos=new BlockPos(1,1,1);var core=tank(helper,pos);
        helper.setBlock(new BlockPos(1,1,2),PressureContent.TANK_FLUID_OUTPUT.get());
        helper.setBlock(new BlockPos(1,1,3),PressureContent.WATER_SOURCE.get());
        helper.runAfterDelay(25,()->{
            var player=FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("pressure-menu-test".getBytes()),"PressureMenu"));
            player.setPos(Vec3.atCenterOf(core.getBlockPos()));player.getInventory().clearContent();
            var menu=new net.bdew.pressure.blocks.tank.TankMenu(1,player.getInventory(),core.getBlockPos());
            menu.setCarried(new ItemStack(Items.LAVA_BUCKET));
            menu.clicked(2,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            helper.assertTrue(core.getFluidFilter()==Fluids.LAVA,"Ghost filter must set the fluid");
            helper.assertTrue(menu.getCarried().is(Items.LAVA_BUCKET),"Ghost filter must not consume the container");
            helper.assertTrue(core.getTank().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Tank must enforce selected filter");
            menu.setCarried(ItemStack.EMPTY);menu.clicked(2,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            helper.assertTrue(core.getFluidFilter()==null,"Empty cursor clears filter");
            player.getInventory().setItem(9,new ItemStack(Items.WATER_BUCKET));menu.quickMoveStack(player,3);
            helper.assertTrue(core.getInventory().getStackInSlot(0).is(Items.WATER_BUCKET),"Shift click must move a fluid container into input");
            helper.assertTrue(core.getOutput(0)!=null,"Connected outlet must occupy an output channel");
            helper.assertTrue(menu.clickMenuButton(player,0),"Output redstone button must be accepted");
            var saved=core.saveWithoutMetadata();core.load(saved);
            helper.assertTrue(core.getOutput(0).mode==net.bdew.pressure.blocks.router.RouterRedstoneMode.RS_ON,"Output mode must persist");
            helper.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void pressureRingInvalidatesAfterBreakAndReconnect(GameTestHelper helper) {
        for(int i=1;i<=5;i++) for(BlockPos p:java.util.List.of(new BlockPos(i,1,1),new BlockPos(i,1,5),new BlockPos(1,1,i),new BlockPos(5,1,i)))
            helper.setBlock(p,PressureContent.PIPE.get());
        helper.setBlock(new BlockPos(0,1,1),PressureContent.INPUT.get().defaultBlockState().setValue(net.bdew.pressure.blocks.InputBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(6,1,5),PressureContent.OUTPUT.get().defaultBlockState().setValue(net.bdew.pressure.blocks.OutputBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(7,1,5),PressureContent.TANK_CONTROLLER.get());
        helper.setBlock(new BlockPos(7,1,6),PressureContent.TANK_BLOCK.get());
        helper.runAfterDelay(25,()->{
            var input=helper.getBlockEntity(new BlockPos(0,1,1)).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
            var core=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(7,1,5));
            helper.assertTrue(input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==1000,"Ring must carry fluid to output");
            helper.assertTrue(core.getTank().getFluidAmount()==1000,"Ring must not duplicate fluid via both paths");
            helper.setBlock(new BlockPos(3,1,1),Blocks.AIR);
            helper.setBlock(new BlockPos(3,1,5),Blocks.AIR);
            helper.assertTrue(input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Broken ring must invalidate cached route");
            helper.setBlock(new BlockPos(3,1,5),PressureContent.PIPE.get());
            helper.assertTrue(input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==1000,"Reconnected route must work");
            helper.succeed();
        });
    }

    @GameTest(template="empty")
    public static void drainVoidsButSluiceBackpressuresAndBothFilter(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1,1,1),PressureContent.DRAIN.get().defaultBlockState().setValue(net.bdew.pressure.blocks.DrainBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(1,1,3),PressureContent.SLUICE.get().defaultBlockState().setValue(net.bdew.pressure.blocks.SluiceBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(2,1,1),Blocks.STONE);
        helper.setBlock(new BlockPos(2,1,3),Blocks.STONE);
        var drain=helper.getBlockEntity(new BlockPos(1,1,1)).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        var sluice=helper.getBlockEntity(new BlockPos(1,1,3)).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        helper.assertTrue(drain.fill(new FluidStack(Fluids.WATER,2000000),FluidAction.EXECUTE)==2000000,"Drain must accept excess with a blocked outlet");
        helper.assertTrue(sluice.fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE)==1000,"Sluice buffers one bucket");
        helper.assertTrue(sluice.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Full blocked sluice must backpressure");
        helper.getBlockEntity(new BlockPos(1,1,1)).getCapability(net.bdew.pressure.api.PressureAPI.FILTERABLE).ifPresent(f->f.setFluidFilter(Fluids.LAVA));
        helper.assertTrue(drain.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Drain capability must respect filter");
        helper.setBlock(new BlockPos(2,1,3),Blocks.AIR);
        sluice.fill(new FluidStack(Fluids.WATER,1),FluidAction.EXECUTE);
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(2,1,3))).isSource(),"Unblocked sluice must release its buffer");
        helper.succeed();
    }
    private static ItemStack canister(int amount) {
        return canister(Fluids.WATER, amount);
    }
    private static ItemStack canister(net.minecraft.world.level.material.Fluid fluid,int amount) {
        ItemStack stack = new ItemStack(PressureContent.CANISTER.get());
        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new)
                .fill(new FluidStack(fluid, amount), FluidAction.EXECUTE);
        return stack;
    }
    private static int amount(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new).getFluidInTank(0).getAmount();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void waterAndLavaRepeatedPlacementAndBuildHeight(GameTestHelper helper) {
        int row=0;
        var sources=new java.util.ArrayList<BlockPos>();
        for(var fluid:java.util.List.of(Fluids.WATER,Fluids.LAVA)){
            var player=FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("pressure-fluid-"+row).getBytes()),"PressureFluid"+row));
            player.setItemInHand(InteractionHand.MAIN_HAND,canister(fluid,2500));
            for(int i=0;i<3;i++){
                BlockPos floor=helper.absolutePos(new BlockPos(1+i*2,1,1+row*4));
                helper.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
                for(Direction direction:Direction.Plane.HORIZONTAL)helper.getLevel().setBlockAndUpdate(floor.above().relative(direction),Blocks.GLASS.defaultBlockState());
                player.setPos(Vec3.atCenterOf(floor).add(0,2,0));
                player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
                helper.assertTrue(amount(player.getMainHandItem())==Math.max(500,1500-i*1000),"Repeated placement must consume one bucket until less than a bucket remains");
                if(i<2){
                    sources.add(floor.above());
                    int before=amount(player.getMainHandItem());
                    player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
                    helper.assertTrue(amount(player.getMainHandItem())==before,"Existing fluid source must not consume another bucket");
                }else helper.assertTrue(helper.getLevel().getFluidState(floor.above()).isEmpty(),"Partial bucket must not place a source");
            }
            BlockPos ceiling=helper.absolutePos(new BlockPos(1,1,1+row*4)).atY(helper.getLevel().getMaxBuildHeight()-1);
            helper.getLevel().setBlockAndUpdate(ceiling,Blocks.STONE.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND,canister(fluid,2500));
            player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ceiling),Direction.UP,ceiling,false)));
            helper.assertTrue(amount(player.getMainHandItem())==2500,"Out-of-height placement must not consume fluid");row++;
            BlockPos bottom=ceiling.atY(helper.getLevel().getMinBuildHeight());
            helper.getLevel().setBlockAndUpdate(bottom,Blocks.STONE.defaultBlockState());
            player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(bottom),Direction.DOWN,bottom,false)));
            helper.assertTrue(amount(player.getMainHandItem())==2500,"Below-minimum placement must not consume fluid");
        }
        helper.runAfterDelay(40,()->{
            for(BlockPos pos:sources)helper.assertTrue(helper.getLevel().getFluidState(pos).isSource(),"Water/lava source must survive fluid ticks");
            helper.succeed();
        });
    }

    @GameTest(template="empty",batch="world_border")
    public static void canisterRejectsOutsideWorldBorder(GameTestHelper helper) {
        var border=helper.getLevel().getWorldBorder();double x=border.getCenterX(),z=border.getCenterZ(),size=border.getSize();
        BlockPos floor=helper.absolutePos(new BlockPos(2,1,2));helper.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        var player=FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("pressure-border".getBytes()),"PressureBorder"));
        player.setItemInHand(InteractionHand.MAIN_HAND,canister(2500));
        try{
            border.setCenter(floor.getX()+0.5,floor.getZ()+0.5);border.setSize(1);
            player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.EAST,floor,false)));
            helper.assertTrue(amount(player.getMainHandItem())==2500,"Border rejection must preserve canister contents");
        }finally{border.setCenter(x,z);border.setSize(size);}
        helper.succeed();
    }
    private static TankControllerBlockEntity tank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, PressureContent.TANK_CONTROLLER.get());
        helper.setBlock(pos.east(), PressureContent.TANK_BLOCK.get());
        TankControllerBlockEntity tank=(TankControllerBlockEntity) helper.getBlockEntity(pos);
        tank.getTank();
        return tank;
    }

    @GameTest(template="empty", timeoutTicks=80)
    public static void canisterPlacementConsumesOnceAndPersists(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockPos floor=helper.absolutePos(new BlockPos(2,1,2));
        helper.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        player.setPos(Vec3.atCenterOf(floor).add(0,2,0));
        player.setItemInHand(InteractionHand.MAIN_HAND,canister(2500));
        var hit=new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false);
        var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        helper.assertTrue(result.consumesAction(),"Valid placement must succeed");
        helper.assertTrue(amount(player.getMainHandItem())==1500,"Held result must consume exactly 1000 mB");
        helper.runAfterDelay(25,()->{
            helper.assertTrue(helper.getLevel().getFluidState(floor.above()).isSource(),"Placed source must survive fluid ticks");
            helper.assertTrue(amount(player.getMainHandItem())==1500,"Fluid ticks must not consume twice");
            helper.succeed();
        });
    }

    @GameTest(template="empty")
    public static void canisterRejectsOccupiedAndPartialAndSupportsOffhand(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockPos floor=helper.absolutePos(new BlockPos(2,1,2));
        helper.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(floor.above(),Blocks.STONE.defaultBlockState());
        var hit=new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false);
        player.setItemInHand(InteractionHand.OFF_HAND,canister(2500));
        player.getOffhandItem().useOn(new UseOnContext(player,InteractionHand.OFF_HAND,hit));
        helper.assertTrue(amount(player.getOffhandItem())==2500,"Occupied placement must not drain");
        helper.getLevel().setBlockAndUpdate(floor.above(),Blocks.AIR.defaultBlockState());
        player.setItemInHand(InteractionHand.OFF_HAND,canister(500));
        player.getOffhandItem().useOn(new UseOnContext(player,InteractionHand.OFF_HAND,hit));
        helper.assertTrue(amount(player.getOffhandItem())==500 && helper.getLevel().isEmptyBlock(floor.above()),"Partial bucket must not place");
        player.setItemInHand(InteractionHand.OFF_HAND,canister(2500));
        player.getOffhandItem().useOn(new UseOnContext(player,InteractionHand.OFF_HAND,hit));
        helper.assertTrue(amount(player.getOffhandItem())==1500,"Offhand placement must update the offhand");
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void canisterPreservesTaggedFluidsAndSimulation(GameTestHelper helper) {
        ItemStack stack=canister(1500);
        var handler=stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
        handler.drain(1000,FluidAction.SIMULATE);
        helper.assertTrue(amount(stack)==1500,"Simulation must not mutate the stack");
        handler.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
        var fluid=new FluidStack(Fluids.WATER,1000); fluid.getOrCreateTag().putString("test","preserved");
        handler.fill(fluid,FluidAction.EXECUTE);
        helper.assertTrue(handler.getFluidInTank(0).isFluidEqual(fluid),"Fluid NBT must survive storage");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Different fluid NBT must not mix");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=80)
    public static void tankMembershipTraversesModulesAndDoesNotDoubleCount(GameTestHelper helper) {
        var core=tank(helper,new BlockPos(1,1,1));
        helper.setBlock(new BlockPos(1,1,2),PressureContent.TANK_FLUID_INPUT.get());
        helper.setBlock(new BlockPos(1,1,3),PressureContent.TANK_BLOCK.get());
        helper.setBlock(new BlockPos(3,1,1),PressureContent.TANK_CONTROLLER.get());
        helper.runAfterDelay(25,()->{
            var other=(TankControllerBlockEntity)helper.getBlockEntity(new BlockPos(3,1,1));
            helper.assertTrue(core.getTank().getCapacity()==2*PressureConfig.TANK_CAPACITY_PER_BLOCK.get(),"Modules must bridge tank structure: capacity=" + core.getTank().getCapacity()+", members="+core.getMembers());
            helper.assertTrue(other.getTank().getCapacity()==0,"Adjacent controllers must not share capacity");
            var module=(TankModuleBlockEntity)helper.getBlockEntity(new BlockPos(1,1,3));
            helper.assertTrue(core.getBlockPos().equals(module.getControllerPos()),"Module owner must be synchronized/persisted");
            var saved=module.saveWithoutMetadata(); module.load(saved);
            helper.assertTrue(core.getBlockPos().equals(module.getControllerPos()),"Ownership must survive NBT reload");
            helper.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=80)
    public static void tankProcessesBucketsWithoutLosingFluid(GameTestHelper helper) {
        var core=tank(helper,new BlockPos(1,1,1));
        core.getInventory().setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));
        helper.runAfterDelay(5,()->{
            helper.assertTrue(core.getTank().getFluidAmount()==1000,"Input bucket must add exactly one bucket");
            helper.assertTrue(core.getInventory().getStackInSlot(1).is(Items.BUCKET),"Empty container goes to output");
            core.getInventory().setStackInSlot(1,ItemStack.EMPTY);
            core.getInventory().setStackInSlot(0,new ItemStack(Items.BUCKET));
        });
        helper.runAfterDelay(10,()->{
            helper.assertTrue(core.getTank().isEmpty(),"Filling bucket consumes exactly one bucket");
            helper.assertTrue(core.getInventory().getStackInSlot(1).is(Items.WATER_BUCKET),"Filled bucket goes to output");
            core.getInventory().setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));
        });
        helper.runAfterDelay(15,()->{
            helper.assertTrue(core.getTank().isEmpty(),"Blocked result slot must not alter tank contents");
            helper.assertTrue(core.getInventory().getStackInSlot(0).is(Items.WATER_BUCKET),"Blocked result must keep input");
            helper.assertTrue(core.getCountsIn().getOrDefault("minecraft:water",0L)==1000L,"Tank input count excludes simulated and blocked transfers");
            helper.assertTrue(core.getCountsOut().getOrDefault("minecraft:water",0L)==1000L,"Tank output count records executed transfer only");
            helper.succeed();
        });
    }
}
