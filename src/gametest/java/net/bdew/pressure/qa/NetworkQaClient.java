package net.bdew.pressure.qa;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.blocks.tank.TankMenu;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Files;
import java.util.Map;

@Mod.EventBusSubscriber(modid="pressureqa",value=Dist.CLIENT)
public final class NetworkQaClient {
    private static int phase,ticks,loginRetries;
    private static boolean visitedAway;
    private static int reloadReadyTicks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("pressure.networkQaClient") || event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();ticks++;
        if(phase==0 && (!(mc.screen instanceof TitleScreen) || mc.getOverlay()!=null)){ticks=0;return;}
        if(phase==0 && mc.screen instanceof TitleScreen && ticks>40){
            phase=1;ticks=0;mc.options.pauseOnLostFocus=false;
            ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25595"),
                    new ServerData("Pressure packaged QA","127.0.0.1:25595",false),false);
            return;
        }
        if(phase==1 && ticks%200==0)System.out.println("PRESSURE_NETWORK_QA_CONNECT_STATE screen="
                +(mc.screen==null?"none":mc.screen.getClass().getName())+" overlay="+(mc.getOverlay()==null?"none":mc.getOverlay().getClass().getName()));
        if(phase==1 && ticks%200==0)try{
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(mc.gameDirectory.toPath().resolve("network-connect-state.png"));}
        }catch(Exception error){throw new IllegalStateException(error);}
        if(phase==1 && mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen && loginRetries<3){
            loginRetries++;System.out.println("PRESSURE_NETWORK_QA_LOGIN_RETRY "+loginRetries);
            phase=0;ticks=0;mc.setScreen(new TitleScreen());return;
        }
        if(ticks>(phase==9?6000:2400) && phase<10)throw new IllegalStateException("Network QA timed out at phase "+phase);
        if(phase==10 && ticks>40){phase=11;mc.stop();return;}
        if(mc.level==null || mc.player==null || mc.gameMode==null)return;
        if(phase==9 && mc.player.getX()>NetworkQaServer.CORE.getX()+100)visitedAway=true;
        if(phase==9 && visitedAway && mc.player.getX()<NetworkQaServer.CORE.getX()+10){
            mc.mouseHandler.releaseMouse();mc.player.setYRot(135);mc.player.setXRot(35);
        }
        var core=mc.level.getBlockEntity(NetworkQaServer.CORE) instanceof TankControllerBlockEntity tank?tank:null;
        if(core==null)return;
        boolean primary=mc.player.getGameProfile().getName().equals("PressureQA1");
        boolean placed=mc.level.getFluidState(NetworkQaServer.FLOOR.above()).isSource();
        if(phase==1 && ticks>60){
            if(primary){
                if(!mc.player.getMainHandItem().is(PressureContent.CANISTER.get()))return;
                mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(NetworkQaServer.FLOOR).add(0,0.5,0),Direction.UP,NetworkQaServer.FLOOR,false));
            }
            phase=2;ticks=0;
        }else if(phase==2 && primary && placed && ticks>30){
            int amount=mc.player.getMainHandItem().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(h->h.getFluidInTank(0).getAmount()).orElse(-1);
            if(amount!=1500)throw new IllegalStateException("Client canister amount differs from server expectation: "+amount);
            NetworkQaProtocol.CHANNEL.sendToServer(new NetworkQaProtocol.Stage(1));phase=3;ticks=0;
        }else if(phase==3 && primary && mc.player.containerMenu instanceof TankMenu && mc.player.containerMenu.getCarried().is(Items.LAVA_BUCKET)){
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,2,0,ClickType.PICKUP,mc.player);phase=4;ticks=0;
        }else if(phase==4 && primary && core.getFluidFilter()==Fluids.LAVA){
            if(!mc.player.containerMenu.getCarried().is(Items.LAVA_BUCKET))throw new IllegalStateException("Filter ghost slot consumed the carried bucket");
            mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,0);phase=5;ticks=0;
        }else if((phase==5 && primary || phase==2 && !primary) && placed && core.getFluidFilter()==Fluids.LAVA
                && core.getOutput(0)!=null && core.getOutput(0).mode==RouterRedstoneMode.RS_ON && ticks>30){
            try{
                var dir=mc.gameDirectory.toPath();Files.createDirectories(dir.resolve("screenshots"));
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(dir.resolve("screenshots/network-state.png"));}
                Files.writeString(dir.resolve("network-qa-client.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                        "passed",true,"player",mc.player.getGameProfile().getName(),"placedSource",true,"filter","minecraft:lava",
                        "outputMode","RS_ON","tankAmount",core.getTank().getFluidAmount(),"capacity",core.getTank().getCapacity())));
            }catch(Exception e){throw new IllegalStateException(e);}
            System.out.println("PRESSURE_NETWORK_QA_CLIENT_PASSED "+mc.player.getGameProfile().getName());
            NetworkQaProtocol.CHANNEL.sendToServer(new NetworkQaProtocol.Stage(2));phase=9;ticks=0;
        }else if(phase==9 && visitedAway && mc.player.getX()<NetworkQaServer.CORE.getX()+10 && placed
                && core.getFluidFilter()==Fluids.LAVA && core.getOutput(0)!=null && core.getOutput(0).mode==RouterRedstoneMode.RS_ON
                && ++reloadReadyTicks>=40){
            try{
                var file=mc.gameDirectory.toPath().resolve("network-qa-client.json");
                var report=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();report.addProperty("chunkReloaded",true);
                Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/network-after-chunk-reload.png"));}
            }catch(Exception e){throw new IllegalStateException(e);}
            System.out.println("PRESSURE_NETWORK_QA_CHUNK_RELOAD_PASSED "+mc.player.getGameProfile().getName());
            NetworkQaProtocol.CHANNEL.sendToServer(new NetworkQaProtocol.Stage(3));phase=10;ticks=0;
        }
    }
}
