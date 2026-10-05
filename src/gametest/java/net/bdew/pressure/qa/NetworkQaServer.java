package net.bdew.pressure.qa;

import com.google.gson.GsonBuilder;
import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.blocks.tank.TankMenu;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="pressureqa")
public final class NetworkQaServer {
    public static final BlockPos CORE=new BlockPos(2048,81,0),FLOOR=CORE.east(4);
    private static boolean prepared;
    private static MinecraftServer server;
    private static int reloadTicks;
    private static final Set<String> complete=new HashSet<>();
    private static final Set<String> reloaded=new HashSet<>();
    private static int chunkPhase,chunkTicks;
    private static final Map<String,String> players=new TreeMap<>();
    private static boolean enabled(){return Boolean.getBoolean("pressure.networkQaServer");}

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!enabled() || !(event.getEntity() instanceof ServerPlayer player))return;
        String name=player.getGameProfile().getName();
        if(!name.equals("PressureQA1") && !name.equals("PressureQA2"))throw new IllegalStateException("Unexpected QA player");
        var level=player.serverLevel();server=level.getServer();
        if(!prepared){
            for(int x=-3;x<=7;x++)for(int z=-3;z<=5;z++){
                level.setBlockAndUpdate(new BlockPos(CORE.getX()+x,80,z),Blocks.SMOOTH_STONE.defaultBlockState());
                for(int y=81;y<86;y++)level.setBlockAndUpdate(new BlockPos(CORE.getX()+x,y,z),Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(CORE,PressureContent.TANK_CONTROLLER.get().defaultBlockState());
            level.setBlockAndUpdate(CORE.east(),PressureContent.TANK_BLOCK.get().defaultBlockState());
            level.setBlockAndUpdate(CORE.east(2),PressureContent.TANK_INDICATOR.get().defaultBlockState());
            level.setBlockAndUpdate(CORE.south(),PressureContent.TANK_FLUID_OUTPUT.get().defaultBlockState());
            level.setBlockAndUpdate(CORE.south(2),PressureContent.WATER_SOURCE.get().defaultBlockState());
            level.setBlockAndUpdate(FLOOR,Blocks.STONE.defaultBlockState());
            for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL)
                level.setBlockAndUpdate(FLOOR.above().relative(side),Blocks.GLASS.defaultBlockState());
            var core=(TankControllerBlockEntity)level.getBlockEntity(CORE);
            core.getTank().fill(new FluidStack(Fluids.WATER,12000),FluidAction.EXECUTE);
            level.setDayTime(1000);prepared=true;
        }
        players.put(name,player.getUUID().toString());
        player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
        player.connection.teleport(CORE.getX()+3,84,name.endsWith("1")?2:3,135,30);
        player.getInventory().clearContent();player.getInventory().selected=0;
        ItemStack canister=new ItemStack(PressureContent.CANISTER.get());
        canister.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(h->h.fill(new FluidStack(Fluids.WATER,2500),FluidAction.EXECUTE));
        player.setItemInHand(InteractionHand.MAIN_HAND,canister);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        player.inventoryMenu.broadcastChanges();
        System.out.println("PRESSURE_NETWORK_QA_LOGIN "+name);
    }

    public static void stage(ServerPlayer player,int stage){
        if(!enabled() || player==null)return;
        var core=(TankControllerBlockEntity)player.level().getBlockEntity(CORE);
        if(stage==1 && player.getGameProfile().getName().equals("PressureQA1")){
            check(player.level().getFluidState(FLOOR.above()).isSource(),"Server did not retain placed fluid");
            check(amount(player.getMainHandItem())==1500,"Server canister amount differs from 1500");
            NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inventory,p)->new TankMenu(id,inventory,CORE),
                    Component.translatable("pressure.gui.tank.title")),data->{data.writeBlockPos(CORE);data.writeNbt(core.getUpdateTag());});
            player.containerMenu.setCarried(new ItemStack(Items.LAVA_BUCKET));player.containerMenu.broadcastChanges();
        }else if(stage==2){
            check(core.getFluidFilter()==Fluids.LAVA,"Server filter must be lava");
            check(core.getOutput(0)!=null && core.getOutput(0).mode==RouterRedstoneMode.RS_ON,"Server output mode must be RS_ON");
            complete.add(player.getGameProfile().getName());
            System.out.println("PRESSURE_NETWORK_QA_CLIENT_PASSED "+player.getGameProfile().getName());
            if(complete.size()==2){
                chunkPhase=1;chunkTicks=0;
                for(var online:server.getPlayerList().getPlayers())online.connection.teleport(CORE.getX()+320,84,2,135,30);
                System.out.println("PRESSURE_NETWORK_QA_WAITING_FOR_CHUNK_UNLOAD");
            }
        }else if(stage==3 && chunkPhase==2){
            check(core.getFluidFilter()==Fluids.LAVA && core.getTank().getFluidAmount()==12000,"Reloaded server tank differs");
            reloaded.add(player.getGameProfile().getName());
            if(reloaded.size()==2){
                write("network-qa-server.json",Map.of("passed",true,"players",players,"fluidAmount",core.getTank().getFluidAmount(),
                        "capacity",core.getTank().getCapacity(),"filter","minecraft:lava","outputMode","RS_ON","canisterAmount",1500,"chunkReloaded",true));
                server.halt(false);
            }
        }
    }
    private static int amount(ItemStack stack){return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(h->h.getFluidInTank(0).getAmount()).orElse(-1);}
    private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException("PACKAGED_NETWORK_QA: "+message);}
    private static void write(String file,Object value){
        try{Files.writeString(Path.of(file),new GsonBuilder().setPrettyPrinting().create().toJson(value));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(Boolean.getBoolean("pressure.networkQaReload"))server=event.getServer();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(enabled() && server!=null && event.phase==TickEvent.Phase.END && chunkPhase==1){
            if(++chunkTicks<180)return;
            var level=server.overworld();
            if(level.hasChunkAt(CORE)){
                if(chunkTicks>600)throw new IllegalStateException("QA chunk did not unload after both players left");
                return;
            }
            System.out.println("PRESSURE_NETWORK_QA_CHUNK_UNLOADED");
            var restored=(TankControllerBlockEntity)level.getBlockEntity(CORE);
            check(restored!=null && restored.getTank().getFluidAmount()==12000,"Tank contents did not survive chunk unload");
            check(restored.getFluidFilter()==Fluids.LAVA,"Tank filter did not survive chunk unload");
            chunkPhase=2;chunkTicks=0;
            for(var player:server.getPlayerList().getPlayers())player.connection.teleport(CORE.getX()+3,84,player.getGameProfile().getName().endsWith("1")?2:3,135,30);
        }
        if(!Boolean.getBoolean("pressure.networkQaReload") || server==null || event.phase!=TickEvent.Phase.END || ++reloadTicks!=80)return;
        var level=server.overworld();var core=(TankControllerBlockEntity)level.getBlockEntity(CORE);
        check(core!=null,"Controller absent after server restart");
        check(core.getTank().getFluidAmount()==12000,"Tank content changed across restart");
        check(core.getFluidFilter()==Fluids.LAVA,"Filter did not survive server restart");
        check(core.getOutput(0)!=null && core.getOutput(0).mode==RouterRedstoneMode.RS_ON,"Output mode did not survive restart");
        check(level.getFluidState(FLOOR.above()).isSource(),"Placed fluid did not survive server restart");
        try{
            var report=com.google.gson.JsonParser.parseString(Files.readString(Path.of("network-qa-server.json"))).getAsJsonObject();
            String uuid=report.getAsJsonObject("players").get("PressureQA1").getAsString();
            var tag=NbtIo.readCompressed(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid+".dat").toFile());
            boolean found=false;
            for(Tag item:tag.getList("Inventory",Tag.TAG_COMPOUND)){
                var entry=(net.minecraft.nbt.CompoundTag)item;
                if(entry.getString("id").equals("pressure:canister") && entry.getCompound("tag").getInt("Amount")==1500)found=true;
            }
            check(found,"Player canister amount did not persist");
        }catch(Exception e){throw new IllegalStateException(e);}
        write("network-qa-reload.json",Map.of("passed",true,"tankAmount",12000,"canisterAmount",1500,"filter","minecraft:lava","outputMode","RS_ON","placedSource",true));
        System.out.println("PRESSURE_NETWORK_QA_RELOAD_PASSED");server.halt(false);
    }
}
