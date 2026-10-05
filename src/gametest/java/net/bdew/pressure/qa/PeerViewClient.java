package net.bdew.pressure.qa;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.client.TankScreen;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid="pressureqa",value=Dist.CLIENT)
public final class PeerViewClient {
    private static int phase,ticks,stable,hover,guiScale=2,retries;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("pressure.peerViewClient") || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();ticks++;
        if(phase==1 && mc.screen instanceof DisconnectedScreen && retries<3){
            retries++;phase=0;ticks=0;mc.setScreen(new TitleScreen());return;
        }
        if(phase==4 && ticks>30){phase=5;mc.stop();return;}
        if(ticks>1800 && phase<4){mc.stop();throw new IllegalStateException("Peer view timed out at "+phase);}
        if(phase==0 && mc.screen instanceof TitleScreen && mc.getOverlay()==null && ticks>40){
            mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.getWindow().setWindowed(1280,960);mc.resizeDisplay();
            ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25595"),new ServerData("Pressure saved peer view","127.0.0.1:25595",false),false);
            phase=1;ticks=0;return;
        }
        if(mc.level==null || mc.player==null)return;
        if(!(mc.level.getBlockEntity(NetworkQaServer.CORE) instanceof TankControllerBlockEntity core))return;
        if(phase==1 && ticks>120){
            mc.mouseHandler.releaseMouse();mc.player.setYRot(180);mc.player.setXRot(16);
            capture("peer-indicator-after-restart.png");
            NetworkQaProtocol.CHANNEL.sendToServer(new NetworkQaProtocol.Stage(6));phase=2;ticks=0;
        }else if(phase==2 && mc.screen instanceof TankScreen screen && ticks>40){
            for(int slot=0;slot<6;slot++)if(core.getOutput(slot)==null || core.getOutput(slot).mode!=RouterRedstoneMode.RS_OFF)throw new IllegalStateException("Saved peer output mode mismatch");
            var buttons=screen.children().stream().filter(widget->widget instanceof Button).map(widget->(Button)widget).toList();
            if(buttons.size()!=6 || buttons.stream().anyMatch(button->!button.getMessage().getString().equals("\u2212"))){stable=0;return;}
            if(++stable<5)return; // Include complete rendered frames after the last label update.
            capture("peer-six-output-gui-after-restart.png");
            phase=3;ticks=0;
        }else if(phase==3 && mc.screen instanceof TankScreen && ticks>20){
            capture("peer-output-tooltip-scale"+guiScale+"-"+hover+".png");ticks=0;
            if(++hover>=6){
                if(guiScale==2){guiScale=4;hover=0;mc.options.guiScale().set(guiScale);mc.resizeDisplay();}
                else{System.out.println("PRESSURE_PEER_VIEW_PASSED "+mc.player.getGameProfile().getName());phase=4;}
            }
        }
    }
    @SubscribeEvent public static void render(net.minecraftforge.client.event.ScreenEvent.Render.Pre event){
        if(!Boolean.getBoolean("pressure.peerViewClient") || phase!=3 || !(event.getScreen() instanceof TankScreen screen))return;
        int left=(Minecraft.getInstance().getWindow().getGuiScaledWidth()-176)/2;
        int top=(Minecraft.getInstance().getWindow().getGuiScaledHeight()-166)/2;
        // Exercise the actual screen/tooltip renderer with a deterministic in-screen hover.
        // This changes only the test frame's input, without moving the desktop cursor.
        event.setCanceled(true);
        screen.render(event.getGuiGraphics(),left+43+21*hover+9,top+42+9,event.getPartialTick());
    }
    private static void capture(String name){
        var mc=Minecraft.getInstance();
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            if(image.getWidth()<640 || image.getHeight()<480)throw new IllegalStateException("Invalid peer screenshot dimensions");
            Path directory=mc.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(directory);image.writeToFile(directory.resolve(name));
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
