package net.bdew.pressure.qa;

import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.blockentity.*;
import net.bdew.pressure.blocks.*;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.config.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("pressure")
@PrefixGameTestTemplate(false)
public final class RevisionGameTests {
    @GameTest(template="empty")
    public static void shiftPlacementOnlyReversesFlowMachines(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(2,2,2));
        for (var entry : PressureContent.BLOCKS.getEntries()) {
            Block block = entry.get();
            if (!block.defaultBlockState().hasProperty(DirectionalMachineBlock.FACING)) continue;
            for (Direction direction : Direction.values()) {
                player.setYRot(direction.toYRot());
                player.setXRot(direction == Direction.UP ? -90 : direction == Direction.DOWN ? 90 : 0);
                player.setPos(Vec3.atCenterOf(pos).subtract(Vec3.atLowerCornerOf(direction.getNormal()).scale(3)));
                var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                player.setShiftKeyDown(false);
                var normal = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), hit));
                player.setShiftKeyDown(true);
                var shifted = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), hit));
                Direction expected = normal.getValue(DirectionalMachineBlock.FACING);
                if (block instanceof ReversibleFlowPlacement) expected = expected.getOpposite();
                helper.assertTrue(shifted.getValue(DirectionalMachineBlock.FACING) == expected, entry.getId()+" Shift facing "+direction);
                helper.getLevel().setBlockAndUpdate(pos, shifted);
                var saved = net.minecraft.nbt.NbtUtils.writeBlockState(shifted);
                var restored = net.minecraft.nbt.NbtUtils.readBlockState(helper.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), saved);
                helper.assertTrue(restored.getValue(DirectionalMachineBlock.FACING) == expected, "Facing must survive serialization");
            }
        }
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void infiniteWaterBucketInventoryMatrix(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2,1,2), PressureContent.WATER_SOURCE.get());
        var pos = helper.absolutePos(new BlockPos(2,1,2));
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("revision-buckets".getBytes()), "PressureBuckets")) {
            // Forge's default FakePlayer suppresses drops; reproduce a real player's full-inventory delivery.
            @Override public net.minecraft.world.entity.item.ItemEntity drop(ItemStack stack, boolean random, boolean includeName) {
                var item=new net.minecraft.world.entity.item.ItemEntity(level(),getX(),getY(),getZ(),stack.copy());
                level().addFreshEntity(item);return item;
            }
        };
        for (boolean creative : new boolean[]{false,true}) for (var hand : InteractionHand.values())
            for (int count : new int[]{1,3}) for (boolean full : new boolean[]{false,true}) {
                player.getInventory().clearContent(); player.getInventory().selected = 0;
                player.getAbilities().instabuild = creative;
                if (full) for (int slot=0; slot<36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE,64));
                player.setItemInHand(hand, new ItemStack(Items.BUCKET,count));
                long beforeDrops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        player.getBoundingBox().inflate(10)).stream().filter(item->item.getItem().is(Items.WATER_BUCKET)).count();
                var state = helper.getLevel().getBlockState(pos);
                var result = state.use(helper.getLevel(),player,hand,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                helper.assertTrue(result.consumesAction(),"Bucket interaction must succeed");
                if (creative) helper.assertTrue(player.getItemInHand(hand).is(Items.BUCKET) && player.getItemInHand(hand).getCount()==count,"Creative must retain empties");
                else if (count==1) helper.assertTrue(player.getItemInHand(hand).is(Items.WATER_BUCKET),"Single empty is replaced in its hand");
                else helper.assertTrue(player.getItemInHand(hand).is(Items.BUCKET) && player.getItemInHand(hand).getCount()==count-1,"Survival consumes one empty");
                int inventory = player.getInventory().items.stream().filter(stack->stack.is(Items.WATER_BUCKET)).mapToInt(ItemStack::getCount).sum()
                        + (player.getOffhandItem().is(Items.WATER_BUCKET)?player.getOffhandItem().getCount():0);
                long drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        player.getBoundingBox().inflate(10)).stream().filter(item->item.getItem().is(Items.WATER_BUCKET)).count()-beforeDrops;
                helper.assertTrue(inventory+drops==1,"Exactly one filled bucket: creative="+creative+" hand="+hand+" count="+count+" full="+full+" inventory="+inventory+" drops="+drops);
                helper.assertTrue(helper.getLevel().getBlockState(pos).is(PressureContent.WATER_SOURCE.get()) && helper.getLevel().getFluidState(pos).isEmpty(),"Source cannot deplete or place water");
            }
        player.getInventory().clearContent(); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        helper.assertTrue(helper.getLevel().getBlockState(pos).use(helper.getLevel(),player,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))==InteractionResult.PASS,"Other interactions must pass");
        player.getAbilities().instabuild=false;
        helper.succeed();
    }

    private static TankControllerBlockEntity tank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos,PressureContent.TANK_CONTROLLER.get());
        helper.setBlock(pos.south(),PressureContent.TANK_BLOCK.get());
        var core=(TankControllerBlockEntity)helper.getBlockEntity(pos);core.getTank();return core;
    }

    @GameTest(template="empty",batch="revision_facings")
    public static void reversedFlowFacesTransferInEveryDirection(GameTestHelper helper) {
        BlockPos machinePos=new BlockPos(3,3,3);
        for(Block block:List.of(PressureContent.PUMP.get(),PressureContent.INPUT.get(),PressureContent.OUTPUT.get(),
                PressureContent.CHECK_VALVE.get(),PressureContent.PIPE_SENSOR.get())) for(Direction facing:Direction.values()) {
            var state=block.defaultBlockState().setValue(DirectionalMachineBlock.FACING,facing);
            helper.setBlock(machinePos,state);
            boolean network=block==PressureContent.INPUT.get() || block==PressureContent.CHECK_VALVE.get() || block==PressureContent.PIPE_SENSOR.get();
            BlockPos receiver=machinePos.relative(facing,network?2:1);
            Direction lateral=facing.getAxis()==Direction.Axis.Y?Direction.EAST:Direction.UP;
            if(network)helper.setBlock(machinePos.relative(facing),PressureContent.OUTPUT.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,facing));
            helper.setBlock(receiver,PressureContent.TANK_CONTROLLER.get());helper.setBlock(receiver.relative(lateral),PressureContent.TANK_BLOCK.get());
            var sink=(TankControllerBlockEntity)helper.getBlockEntity(receiver);sink.getTank();
            var machine=helper.getBlockEntity(machinePos);
            java.util.function.Function<Boolean,Integer> transfer=execute->{
                var fluid=new FluidStack(Fluids.WATER,12345);
                if(machine instanceof net.bdew.pressure.api.IPressureEject endpoint)return endpoint.eject(fluid,facing.getOpposite(),execute);
                return machine.getCapability(ForgeCapabilities.FLUID_HANDLER,facing.getOpposite()).orElseThrow(IllegalStateException::new)
                        .fill(fluid,execute?FluidAction.EXECUTE:FluidAction.SIMULATE);
            };
            helper.assertTrue(transfer.apply(false)==12345 && sink.getTank().isEmpty(),"Simulation for "+block+" "+facing);
            helper.assertTrue(transfer.apply(true)==12345 && sink.getTank().getFluidAmount()==12345,"Actual facing selects flow path for "+block+" "+facing);
            helper.setBlock(receiver,Blocks.AIR);helper.setBlock(receiver.relative(lateral),Blocks.AIR);
            helper.setBlock(machinePos,Blocks.AIR);if(network)helper.setBlock(machinePos.relative(facing),Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="revision_output_faces")
    public static void activeOutputsAllFacesPartialBlockedAndCounts(GameTestHelper helper) {
        BlockPos outlet=new BlockPos(3,3,3);
        for(Block block:List.of(PressureContent.TANK_FLUID_OUTPUT.get(),PressureContent.TANK_FLUID_OUTPUT_AUTO.get()))for(Direction direction:Direction.values()) {
            Direction lateral=direction.getAxis()==Direction.Axis.Y?Direction.EAST:Direction.UP;
            BlockPos corePos=outlet.relative(direction.getOpposite()),receiver=outlet.relative(direction);
            helper.setBlock(corePos,PressureContent.TANK_CONTROLLER.get());helper.setBlock(corePos.relative(lateral),PressureContent.TANK_BLOCK.get());
            helper.setBlock(outlet,block);
            var core=(TankControllerBlockEntity)helper.getBlockEntity(corePos);core.getTank();
            helper.setBlock(receiver,PressureContent.TANK_CONTROLLER.get());helper.setBlock(receiver.relative(lateral),PressureContent.TANK_BLOCK.get());
            var sink=(TankControllerBlockEntity)helper.getBlockEntity(receiver);sink.getTank();
            var module=(TankModuleBlockEntity)helper.getBlockEntity(outlet);
            TankControllerBlockEntity.serverTick(helper.getLevel(),core.getBlockPos(),core.getBlockState(),core);
            var cfg=core.outputAt(module.getBlockPos(),direction);helper.assertTrue(cfg!=null,"Discover output "+direction);
            for(RouterRedstoneMode mode:RouterRedstoneMode.values())for(boolean signal:new boolean[]{false,true}) {
                helper.setBlock(outlet.relative(lateral.getOpposite()),signal?Blocks.REDSTONE_BLOCK:Blocks.AIR);cfg.mode=mode;
                core.getTank().setFluid(new FluidStack(Fluids.WATER,5000));
                sink.getTank().setFluid(new FluidStack(Fluids.WATER,sink.getTank().getCapacity()-777));
                long before=core.getCountsOut().getOrDefault("minecraft:water",0L);
                TankModuleBlockEntity.serverTick(helper.getLevel(),module.getBlockPos(),module.getBlockState(),module);
                int moved=mode.allows(signal)?777:0;
                helper.assertTrue(core.getTank().getFluidAmount()==5000-moved,"Partial receiver/redstone "+direction+" "+mode+" "+signal);
                helper.assertTrue(core.getCountsOut().getOrDefault("minecraft:water",0L)-before==moved,"Executed output counts only");
                TankModuleBlockEntity.serverTick(helper.getLevel(),module.getBlockPos(),module.getBlockState(),module);
                helper.assertTrue(core.getTank().getFluidAmount()==5000-moved,"Full receiver cannot lose fluid");
            }
            helper.setBlock(receiver,Blocks.AIR);helper.setBlock(receiver.relative(lateral),Blocks.AIR);
            core.getTank().setFluid(new FluidStack(Fluids.WATER,5000));
            TankModuleBlockEntity.serverTick(helper.getLevel(),module.getBlockPos(),module.getBlockState(),module);
            helper.assertTrue(core.getTank().getFluidAmount()==5000,"No target cannot lose fluid");
            helper.setBlock(corePos,Blocks.AIR);helper.setBlock(corePos.relative(lateral),Blocks.AIR);helper.setBlock(outlet,Blocks.AIR);
            helper.setBlock(outlet.relative(lateral.getOpposite()),Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="revision_throughput")
    public static void unrestrictedAndExplicitLimitsConserveAtIntegerBoundary(GameTestHelper helper) {
        int oldRate=PressureConfig.FLUID_TRANSFER_PER_TICK.get(),oldCapacity=PressureConfig.TANK_CAPACITY_PER_BLOCK.get();
        System.out.println("PRESSURE_EFFECTIVE_TRANSFER_CONFIG="+oldRate);
        PressureConfig.TANK_CAPACITY_PER_BLOCK.set(Integer.MAX_VALUE);
        try {
            for(String route:List.of("pump","input","pump+input")) {
                var source=tank(helper,new BlockPos(1,1,1));
                helper.setBlock(new BlockPos(2,1,1),route.equals("input")?PressureContent.INPUT.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.EAST)
                        :PressureContent.PUMP.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.EAST));
                BlockPos sinkPos=new BlockPos(route.equals("pump")?3:route.equals("input")?4:5,1,1);
                if(route.equals("pump+input"))helper.setBlock(new BlockPos(3,1,1),PressureContent.INPUT.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.EAST));
                if(!route.equals("pump"))helper.setBlock(sinkPos.west(),PressureContent.OUTPUT.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.EAST));
                var sink=tank(helper,sinkPos);
                var machine=(PressureNodeBlockEntity)helper.getBlockEntity(new BlockPos(2,1,1));
                Runnable tick=()->{
                    if(machine instanceof PumpBlockEntity pump)PumpBlockEntity.serverTick(helper.getLevel(),pump.getBlockPos(),pump.getBlockState(),pump);
                    else if(machine instanceof InputBlockEntity input)InputBlockEntity.serverTick(helper.getLevel(),input.getBlockPos(),input.getBlockState(),input);
                };
                helper.setBlock(new BlockPos(2,2,1),Blocks.REDSTONE_BLOCK);
                for(int limit:new int[]{4000,Integer.MAX_VALUE}) for(int amount:new int[]{1_000_000_000,Integer.MAX_VALUE}) {
                    PressureConfig.FLUID_TRANSFER_PER_TICK.set(limit);
                    source.getTank().setFluid(new FluidStack(Fluids.WATER,amount));sink.getTank().setFluid(FluidStack.EMPTY);
                    tick.run();int moved=sink.getTank().getFluidAmount();
                    helper.assertTrue(moved==Math.min(amount,limit),route+" transfer amount "+moved);
                    helper.assertTrue((long)source.getTank().getFluidAmount()+moved==amount,"Finite fluid conservation at integer boundary");
                    System.out.println("PRESSURE_THROUGHPUT route="+route+" limit="+limit+" offered="+amount+" movedPerTick="+moved);
                }
                source.getTank().setFluid(new FluidStack(Fluids.WATER,10000));sink.getTank().setFluid(FluidStack.EMPTY);
                machine.getCapability(PressureAPI.FILTERABLE).ifPresent(f->f.setFluidFilter(Fluids.LAVA));tick.run();
                helper.assertTrue(sink.getTank().isEmpty() && source.getTank().getFluidAmount()==10000,"Filter must conserve rejected fluid");
                machine.getCapability(PressureAPI.FILTERABLE).ifPresent(f->f.clearFluidFilter());
                helper.setBlock(new BlockPos(2,2,1),Blocks.AIR);tick.run();
                helper.assertTrue(sink.getTank().isEmpty(),"Unpowered automatic extraction must stop");
                for(int x=1;x<=6;x++)for(int z=1;z<=2;z++)helper.setBlock(new BlockPos(x,1,z),Blocks.AIR);
            }
            helper.succeed();
        }finally{PressureConfig.FLUID_TRANSFER_PER_TICK.set(oldRate);PressureConfig.TANK_CAPACITY_PER_BLOCK.set(oldCapacity);}
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void bothFluidOutputsPushAndRetainPullRedstoneRules(GameTestHelper helper) {
        var core=tank(helper,new BlockPos(2,1,2));
        BlockPos outlet=new BlockPos(3,1,2),receiver=new BlockPos(4,1,2);
        var sink=tank(helper,receiver);
        helper.setBlock(outlet,PressureContent.TANK_FLUID_OUTPUT.get());
        helper.runAfterDelay(25,()->{
            for(Block block:List.of(PressureContent.TANK_FLUID_OUTPUT.get(),PressureContent.TANK_FLUID_OUTPUT_AUTO.get())) {
                helper.setBlock(outlet,block);
                var module=(TankModuleBlockEntity)helper.getBlockEntity(outlet);module.setControllerPos(core.getBlockPos());
                for(var mode:RouterRedstoneMode.values())for(boolean powered:new boolean[]{false,true}) {
                    helper.setBlock(outlet.above(),powered?Blocks.REDSTONE_BLOCK:Blocks.AIR);
                    var cfg=core.outputAt(module.getBlockPos(),Direction.EAST);
                    helper.assertTrue(cfg!=null,"Output face must be discovered");cfg.mode=mode;
                    core.getTank().setFluid(new FluidStack(Fluids.WATER,12000));sink.getTank().setFluid(FluidStack.EMPTY);
                    var cap=module.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.EAST).orElseThrow(IllegalStateException::new);
                    var simulated=cap.drain(1000,FluidAction.SIMULATE);
                    helper.assertTrue(simulated.getAmount()==(mode.allows(powered)?1000:0) && core.getTank().getFluidAmount()==12000,"External-pull simulation/redstone unchanged");
                    var pulled=cap.drain(1000,FluidAction.EXECUTE);
                    helper.assertTrue(pulled.getAmount()==simulated.getAmount(),"External-pull execution/redstone unchanged");
                    TankModuleBlockEntity.serverTick(helper.getLevel(),module.getBlockPos(),module.getBlockState(),module);
                    int pushed=sink.getTank().getFluidAmount();
                    helper.assertTrue((pushed>0)==mode.allows(powered),"Active push follows same redstone gate");
                    helper.assertTrue(core.getTank().getFluidAmount()+pushed+pulled.getAmount()==12000,"Push/pull conservation");
                    var saved=core.saveWithoutMetadata();core.load(saved);
                    helper.assertTrue(core.outputAt(module.getBlockPos(),Direction.EAST).mode==mode,"Output settings persist");
                }
            }
            helper.succeed();
        });
    }
}
