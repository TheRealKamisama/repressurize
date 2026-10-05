package net.bdew.pressure.qa;

import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import net.bdew.pressure.blockentity.*;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.nio.*;
import java.nio.file.*;

@PrefixGameTestTemplate(false)
public final class ComputerCraftGameTests {
    @GameTest(templateNamespace="pressure",template="empty",batch="computercraft",timeoutTicks=600)
    public static void actualLuaComputerExercisesAllCommandsAndReconnect(GameTestHelper helper) {
        BlockPos corePos=new BlockPos(1,1,3),port=new BlockPos(2,1,3),computerPos=new BlockPos(3,1,3),sensorPos=new BlockPos(4,1,3);
        helper.setBlock(corePos,PressureContent.TANK_CONTROLLER.get());helper.setBlock(corePos.south(),PressureContent.TANK_BLOCK.get());
        helper.setBlock(port,PressureContent.TANK_DATA_PORT.get());
        helper.setBlock(new BlockPos(1,1,2),PressureContent.TANK_FLUID_OUTPUT.get());helper.setBlock(new BlockPos(1,1,1),PressureContent.WATER_SOURCE.get());
        helper.setBlock(sensorPos,PressureContent.PIPE_SENSOR.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(5,1,3),PressureContent.OUTPUT.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(6,1,3),PressureContent.TANK_CONTROLLER.get());helper.setBlock(new BlockPos(6,1,4),PressureContent.TANK_BLOCK.get());
        var core=(TankControllerBlockEntity)helper.getBlockEntity(corePos);core.getTank().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        helper.setBlock(computerPos,net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("computercraft","computer_normal")));
        var computer=(AbstractComputerBlockEntity)helper.getBlockEntity(computerPos);
        Runnable[] poller={null};
        helper.onEachTick(()->{if(poller[0]!=null)poller[0].run();});
        helper.runAfterDelay(25,()->{
            helper.getBlockEntity(new BlockPos(1,1,2)).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.NORTH)
                    .orElseThrow(IllegalStateException::new).drain(250,FluidAction.EXECUTE);
            var serverComputer=computer.createServerComputer();
            Path directory=helper.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("computercraft/computer/"+serverComputer.getID());
            String script="""
                    local function write(name, value) local f=assert(fs.open(name,'w')); f.write(value or 'ok'); f.close() end
                    local function bad(fn, ...) local ok=pcall(fn,...); assert(not ok, 'invalid call accepted') end
                    local ok, err=pcall(function()
                      local t=assert(peripheral.find('pp_tank'), 'missing pp_tank')
                      local s=assert(peripheral.find('pp_sensor'), 'missing pp_sensor')
                      assert(t.isConnected() and t.getCapacity()==32000)
                      assert(t.hasFluid() and t.getFluid().name=='minecraft:water' and t.getFluid().amount==750)
                      local outputs=t.getOutputs(); local n=0
                      for k,v in pairs(outputs) do n=n+1; assert(v.type=='fluid' or v.type=='unconnected') end
                      assert(n==6 and outputs.red.type=='fluid')
                      for _,mode in ipairs({'ALWAYS','RS_ON','RS_OFF','NEVER'}) do assert(t.setOutputMode('ReD',mode)); assert(t.getOutputs().red.mode==mode) end
                      bad(t.setOutputMode,'orange','ALWAYS'); bad(t.setOutputMode,'red','bad'); bad(t.setOutputMode,'green','ALWAYS')
                      assert(t.getFilter()==nil); assert(t.setFilter('minecraft:lava')); assert(t.getFilter()=='minecraft:lava')
                      bad(t.setFilter,'pressure:no_such_fluid'); bad(t.setFilter,'minecraft:empty'); bad(t.setFilter,123)
                      assert(t.getFilter()=='minecraft:lava'); assert(t.setFilter(nil)); assert(t.getFilter()==nil)
                      assert(t.setFilter()); assert(t.setFilter('minecraft:water'))
                      assert(t.getCountsIn()[1].name=='minecraft:water' and t.getCountsIn()[1].count==1000)
                      assert(t.getCountsOut()[1].name=='minecraft:water' and t.getCountsOut()[1].count==250)
                      assert(s.getCounts()[1].name=='minecraft:water' and s.getCounts()[1].count>0)
                      assert(s.getAverages()[1].name=='minecraft:water' and s.getAverages()[1].average>0)
                      write('phase1')
                      while t.isConnected() do sleep(0.05) end
                      bad(t.getCapacity); bad(t.getFluid); bad(t.getFilter); bad(t.setFilter,'minecraft:water')
                      write('phase2')
                      while not t.isConnected() do sleep(0.05) end
                      assert(t.getFluid().amount==750 and t.getFilter()=='minecraft:water' and t.getOutputs().red.mode=='NEVER')
                      write('phase3')
                      while peripheral.find('pp_tank') do sleep(0.05) end
                      write('phase4')
                      while not peripheral.find('pp_tank') do sleep(0.05) end
                      t=peripheral.find('pp_tank'); while not t.isConnected() do sleep(0.05) end
                      assert(t.getCapacity()==32000 and t.getFluid().amount==750)
                      write('passed', textutils.serialize({tank=t.getFluid(), outputs=t.getOutputs(), countsIn=t.getCountsIn(), countsOut=t.getCountsOut(), sensorCounts=s.getCounts(), sensorAverages=s.getAverages()}))
                    end)
                    if not ok then write('failed', tostring(err)) end
                    """;
            try(var channel=serverComputer.createRootMount().openFile("startup.lua",java.util.Set.of(StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE))) {channel.write(ByteBuffer.wrap(script.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
            catch(Exception e){throw new IllegalStateException(e);}
            computer.updateInputsImmediately();serverComputer.turnOn();
            var sensor=(PipeSensorBlockEntity)helper.getBlockEntity(sensorPos);
            int[] phase={0},wait={0};net.minecraft.nbt.CompoundTag[] saved={null};
            poller[0]=()->{
                sensor.eject(new FluidStack(Fluids.WATER,1),Direction.WEST,true);
                try{
                    helper.assertTrue(!Files.exists(directory.resolve("failed")),Files.exists(directory.resolve("failed"))?Files.readString(directory.resolve("failed")):"Lua command failure");
                    if(phase[0]==0 && Files.exists(directory.resolve("phase1"))) {
                        helper.assertTrue(core.getFluidFilter()==Fluids.WATER && core.getOutput(0).mode==net.bdew.pressure.blocks.router.RouterRedstoneMode.NEVER,"Lua changes authoritative tank state");
                        saved[0]=core.saveWithoutMetadata();helper.setBlock(corePos,Blocks.AIR);phase[0]=1;
                    }else if(phase[0]==1 && Files.exists(directory.resolve("phase2"))) {
                        helper.setBlock(corePos,PressureContent.TANK_CONTROLLER.get());
                        ((TankControllerBlockEntity)helper.getBlockEntity(corePos)).load(saved[0]);phase[0]=2;
                    }else if(phase[0]==2 && Files.exists(directory.resolve("phase3"))) {helper.setBlock(port,Blocks.AIR);phase[0]=3;}
                    else if(phase[0]==3 && Files.exists(directory.resolve("phase4")) && ++wait[0]>20) {
                        helper.setBlock(port,PressureContent.TANK_DATA_PORT.get());phase[0]=4;
                    }else if(phase[0]==4 && Files.exists(directory.resolve("passed"))) {
                        System.out.println("PRESSURE_CC_LUA_QA_PASSED "+Files.readString(directory.resolve("passed")));
                        helper.succeed();
                    }
                }catch(Exception e){throw new IllegalStateException(e);}
            };
        });
    }
}
