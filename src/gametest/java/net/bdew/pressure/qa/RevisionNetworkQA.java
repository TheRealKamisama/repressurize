package net.bdew.pressure.qa;

import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Normal packaged two-client extension: actual placement/buckets, Lua mutation and saved restart. */
@Mod.EventBusSubscriber(modid="pressureqa")
public final class RevisionNetworkQA {
    public static final BlockPos PUMP=NetworkQaServer.CORE.east(6),SOURCE=NetworkQaServer.CORE.south(2);
    private static final BlockPos SENSOR=NetworkQaServer.CORE.west(3);
    private static MinecraftServer server;
    private static final Set<String> ready=new HashSet<>(),complete=new HashSet<>();
    private static int reloadTicks;
    public static boolean enabled(){return Boolean.getBoolean("pressure.revisionNetwork");}
    public static void begin(MinecraftServer instance){
        server=instance;var level=server.overworld();
        for(int i=1;i<=5;i++){
            level.setBlockAndUpdate(NetworkQaServer.CORE.north(i),PressureContent.TANK_FLUID_OUTPUT.get().defaultBlockState());
            level.setBlockAndUpdate(NetworkQaServer.CORE.north(i).west(),PressureContent.WATER_SOURCE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(NetworkQaServer.CORE.west(),PressureContent.TANK_DATA_PORT.get().defaultBlockState());
        var cc=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("computercraft","computer_normal"));
        level.setBlockAndUpdate(NetworkQaServer.CORE.west(2),cc.defaultBlockState());
        level.setBlockAndUpdate(SENSOR,PressureContent.PIPE_SENSOR.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.WEST));
        level.setBlockAndUpdate(SENSOR.west(),PressureContent.OUTPUT.get().defaultBlockState().setValue(DirectionalMachineBlock.FACING,Direction.WEST));
        level.setBlockAndUpdate(SENSOR.west(2),PressureContent.TANK_CONTROLLER.get().defaultBlockState());
        level.setBlockAndUpdate(SENSOR.west(2).south(),PressureContent.TANK_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(PUMP.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(PUMP,Blocks.AIR.defaultBlockState());
        for(var player:server.getPlayerList().getPlayers()){
            player.closeContainer();player.connection.teleport(NetworkQaServer.CORE.getX()+5.5,81,2.5,180,0);
            if(player.getGameProfile().getName().equals("PressureQA1")){
                player.getInventory().setItem(1,player.getMainHandItem().copy());
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PressureContent.PUMP.get()));
                player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET,3));
                player.inventoryMenu.broadcastChanges();
            }
        }
        System.out.println("PRESSURE_REVISION_NETWORK_BEGIN");
    }
    public static void stage(ServerPlayer player,int stage){
        var core=(TankControllerBlockEntity)player.level().getBlockEntity(NetworkQaServer.CORE);
        if(stage==4){
            if(player.getGameProfile().getName().equals("PressureQA1")){
                check(player.getOffhandItem().is(Items.BUCKET) && player.getOffhandItem().getCount()==2,"Offhand bucket count on server");
                check(player.getInventory().items.stream().filter(s->s.is(Items.WATER_BUCKET)).mapToInt(ItemStack::getCount).sum()==1,"Filled bucket on server");
                check(player.level().getBlockState(PUMP).is(PressureContent.PUMP.get()) && player.level().getBlockState(PUMP).getValue(DirectionalMachineBlock.FACING)==Direction.NORTH,"Shift placement orientation on server");
            }
            ready.add(player.getGameProfile().getName());
            if(ready.size()==2){
                for(var peer:server.getPlayerList().getPlayers())net.minecraftforge.network.NetworkHooks.openScreen(peer,
                        new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new net.bdew.pressure.blocks.tank.TankMenu(id,inv,NetworkQaServer.CORE),
                                net.minecraft.network.chat.Component.translatable("pressure.gui.tank.title")),
                        data->{data.writeBlockPos(NetworkQaServer.CORE);data.writeNbt(core.getUpdateTag());});
                var computer=(AbstractComputerBlockEntity)player.level().getBlockEntity(NetworkQaServer.CORE.west(2));
                var cc=computer.createServerComputer();
                String script="""
                        local function write(n,v) local f=assert(fs.open(n,'w')); f.write(v or 'ok'); f.close() end
                        local ok,err=pcall(function()
                          local t
                          repeat t=peripheral.find('pp_tank'); sleep(0.05) until t and t.isConnected()
                          local previous=t.getFilter()
                          for _,name in ipairs({'red','green','blue','yellow','cyan','purple'}) do assert(t.getOutputs()[name].type=='fluid'); assert(t.setOutputMode(name,'RS_OFF')) end
                          assert(t.setFilter('minecraft:water'))
                          assert(t.getFluid().amount==12000 and t.getCapacity()==64000)
                          assert(t.getCountsIn()[1].name=='minecraft:water' and t.getCountsIn()[1].count>=12000)
                          local s=assert(peripheral.find('pp_sensor'))
                          assert(s.getCounts()[1].count>0 and s.getAverages()[1].average>0)
                          write('revision-passed',textutils.serialize({filter=t.getFilter(),outputs=t.getOutputs(),fluid=t.getFluid(),counts=t.getCountsIn()}))
                          if previous=='minecraft:water' then write('revision-restart-passed',textutils.serialize({sensorCounts=s.getCounts(),sensorAverages=s.getAverages(),tankCounts=t.getCountsIn()})) end
                        end)
                        if not ok then write('revision-failed',tostring(err)) end
                        """;
                try(var channel=cc.createRootMount().openFile("startup.lua",Set.of(StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE))){channel.write(ByteBuffer.wrap(script.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
                catch(Exception e){throw new IllegalStateException(e);}
                computer.updateInputsImmediately();cc.turnOn();
                System.out.println("PRESSURE_REVISION_NETWORK_LUA_START id="+cc.getID());
            }
        }else if(stage==5){
            check(core.getFluidFilter()==Fluids.WATER,"Lua filter on server");
            for(int i=0;i<6;i++)check(core.getOutput(i)!=null && core.getOutput(i).mode==RouterRedstoneMode.RS_OFF,"Lua output "+i+" on server");
            complete.add(player.getGameProfile().getName());
            if(complete.size()==2){
                write("revision-network-server.json",Map.of("passed",true,"filter","minecraft:water","sixOutputs","RS_OFF","pumpFacing","north","offhandEmptyBuckets",2,"waterBuckets",1));
                System.out.println("PRESSURE_REVISION_NETWORK_SERVER_PASSED");server.halt(false);
            }
        }
    }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException("REVISION_NETWORK_QA: "+message);}
    private static void write(String name,Object value){try{Files.writeString(Path.of(name),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(value));}catch(Exception e){throw new IllegalStateException(e);}}
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(Boolean.getBoolean("pressure.revisionNetworkReload")){
            server=event.getServer();
            // With no connected players these remote chunks otherwise load without ticking.
            // Force the same simulation coverage as the paired-client pass while checking restart.
            for(int x=127;x<=128;x++)for(int z=-1;z<=0;z++)server.overworld().setChunkForced(x,z,true);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(!Boolean.getBoolean("pressure.revisionNetworkReload") || server==null || event.phase!=TickEvent.Phase.END || ++reloadTicks!=100)return;
        var level=server.overworld();var core=(TankControllerBlockEntity)level.getBlockEntity(NetworkQaServer.CORE);
        check(core!=null && core.getTank().getFluidAmount()==12000 && core.getTank().getCapacity()==64000,"Tank contents after restart");
        check(core.getFluidFilter()==Fluids.WATER,"Computer filter persisted");
        for(int i=0;i<6;i++)check(core.getOutput(i)!=null && core.getOutput(i).mode==RouterRedstoneMode.RS_OFF,"Computer mode persisted "+i);
        check(level.getBlockState(PUMP).is(PressureContent.PUMP.get()) && level.getBlockState(PUMP).getValue(DirectionalMachineBlock.FACING)==Direction.NORTH,"Shift block persisted");
        check(level.getFluidState(NetworkQaServer.FLOOR.above()).isSource(),"Original canister source persisted");
        var sensor=(net.bdew.pressure.blockentity.PipeSensorBlockEntity)level.getBlockEntity(SENSOR);
        check(sensor!=null && sensor.getCounts().getOrDefault("minecraft:water",0L)>0,"Sensor counters persisted before new transfer");
        var cc=(AbstractComputerBlockEntity)level.getBlockEntity(NetworkQaServer.CORE.west(2));
        var directory=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("computercraft/computer/"+cc.getComputerID());
        check(Files.exists(directory.resolve("startup.lua")),"Computer program persisted");
        cc.createServerComputer().turnOn();cc.updateInputsImmediately();
    }
    @SubscribeEvent public static void flow(TickEvent.ServerTickEvent event){
        if(server==null || event.phase!=TickEvent.Phase.END || !(enabled() || Boolean.getBoolean("pressure.revisionNetworkReload") && reloadTicks>=100))return;
        if(server.overworld().getBlockEntity(SENSOR) instanceof net.bdew.pressure.blockentity.PipeSensorBlockEntity sensor)
            sensor.eject(new net.minecraftforge.fluids.FluidStack(Fluids.WATER,1),Direction.EAST,true);
    }
    @SubscribeEvent public static void verifyComputerRestart(TickEvent.ServerTickEvent event){
        if(!Boolean.getBoolean("pressure.revisionNetworkReload") || server==null || event.phase!=TickEvent.Phase.END || reloadTicks<100)return;
        var level=server.overworld();var cc=(AbstractComputerBlockEntity)level.getBlockEntity(NetworkQaServer.CORE.west(2));
        var directory=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("computercraft/computer/"+cc.getComputerID());
        if(!Files.exists(directory.resolve("revision-restart-passed"))){check(reloadTicks<400,"Lua computer restart timeout");return;}
        try{
            var historical=com.google.gson.JsonParser.parseString(Files.readString(Path.of("network-qa-server.json"))).getAsJsonObject();
            String uuid=historical.getAsJsonObject("players").get("PressureQA1").getAsString();
            var tag=net.minecraft.nbt.NbtIo.readCompressed(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(uuid+".dat").toFile());
            boolean canister=false,bucket=false,empties=false;
            for(var entry:tag.getList("Inventory",net.minecraft.nbt.Tag.TAG_COMPOUND)){
                var item=(net.minecraft.nbt.CompoundTag)entry;
                if(item.getString("id").equals("pressure:canister") && item.getCompound("tag").getInt("Amount")==1500)canister=true;
                if(item.getString("id").equals("minecraft:water_bucket") && item.getByte("Count")==1)bucket=true;
                if(item.getByte("Slot")==-106 && item.getString("id").equals("minecraft:bucket") && item.getByte("Count")==2)empties=true;
            }
            check(canister && bucket && empties,"Player inventory persistence");
            write("revision-network-restart.json",Map.of("passed",true,"luaRestart",true,"pumpFacing","north","sixOutputs","RS_OFF","filter","minecraft:water","tankAmount",12000,"canisterAmount",1500,"waterBuckets",1,"offhandEmptyBuckets",2));
        }catch(Exception e){throw new IllegalStateException(e);}
        for(int x=127;x<=128;x++)for(int z=-1;z<=0;z++)level.setChunkForced(x,z,false);
        System.out.println("PRESSURE_REVISION_NETWORK_RESTART_PASSED");server.halt(false);
    }
}
