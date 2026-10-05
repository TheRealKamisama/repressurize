package net.bdew.pressure.qa;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blocks.tank.TankMenu;
import net.bdew.pressure.client.TankScreen;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import java.nio.file.*;
import java.util.*;

/** Matched before/after scenes and real GUI hit tests. Opt-in; never distributed. */
@Mod.EventBusSubscriber(modid="pressureqa",value=Dist.CLIENT)
public final class RevisionVisualQA {
    private static final BlockPos CORE=new BlockPos(0,81,0),GUI_CORE=new BlockPos(0,81,6);
    private static int phase,ticks,index,scale=1,outputCount,button;
    private static final int[] COUNTS={0,2,6};
    private static boolean baseline(){return Boolean.getBoolean("pressure.revisionBaseline");}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("pressure.revisionQa") || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();ticks++;
        if(phase==0 && mc.screen instanceof TitleScreen && mc.getOverlay()==null && ticks>40){
            phase=1;ticks=0;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"Migration QA");return;
        }
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null)return;
        if(ticks>2400)throw new IllegalStateException("Revision visual fixture stalled at "+phase);
        if(phase==1 && ticks>80){
            mc.getSingleplayerServer().execute(()->setup());phase=2;ticks=0;
        }else if(phase==2 && ticks>120){VisualModelAssertions.verify();index=Boolean.getBoolean("pressure.singleIndicatorQa")?18:0;scene(index);phase=3;ticks=0;}
        else if(phase==3 && ticks>60){
            capture("r4-"+String.format("%02d",index)+"-"+sceneName(index)+".png");
            if(++index<21){scene(index);ticks=0;}
            else if(Boolean.getBoolean("pressure.singleIndicatorQa")){System.out.println("PRESSURE_SINGLE_INDICATOR_QA_PASSED");phase=9;mc.stop();}
            else {phase=4;ticks=0;index=0;mc.options.guiScale().set(scale);mc.resizeDisplay();openGui(0);}
        }else if(phase==4 && ticks>60 && mc.screen instanceof TankScreen){
            capture("r7-scale"+scale+"-outputs"+COUNTS[outputCount]+".png");
            if(++outputCount<COUNTS.length){openGui(COUNTS[outputCount]);ticks=0;}
            else if(scale<4){scale++;outputCount=0;mc.options.guiScale().set(scale);mc.resizeDisplay();openGui(0);ticks=0;}
            else {phase=5;ticks=0;button=0;}
        }else if(phase==5 && ticks>20 && mc.screen instanceof TankScreen screen){
            if(baseline()){phase=8;ticks=0;return;}
            var controls=screen.children().stream().filter(widget->widget instanceof Button).map(widget->(Button)widget).toList();
            if(controls.size()!=6)throw new IllegalStateException("Six output controls required");
            var control=controls.get(button);
            int left=(mc.getWindow().getGuiScaledWidth()-176)/2,top=(mc.getWindow().getGuiScaledHeight()-166)/2;
            if(control.getX()!=left+43+21*button || control.getY()!=top+63 || control.getWidth()!=18 || control.getHeight()!=18)
                throw new IllegalStateException("Output button does not align with slot "+button);
            screen.mouseClicked(control.getX()-0.25,control.getY()+9,0);
            if(!screen.mouseClicked(control.getX()+17.75,control.getY()+17.75,0))throw new IllegalStateException("Inside edge must click button");
            phase=6;ticks=0;
        }else if(phase==6 && ticks>20 && mc.screen instanceof TankScreen screen){
            if(screen.getMenu().controller.getOutput(button).mode!=net.bdew.pressure.blocks.router.RouterRedstoneMode.RS_ON)
                throw new IllegalStateException("Button "+button+" did not synchronize authoritative state");
            if(++button<6){phase=5;ticks=0;}else{capture("r7-six-buttons-clicked.png");phase=8;ticks=0;}
        }else if(phase==8 && ticks>40){
            if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))JeiQaPlugin.verify();
            mc.player.closeContainer();mc.setScreen(null);
            System.out.println("PRESSURE_REVISION_VISUAL_QA_PASSED baseline="+baseline());phase=9;mc.stop();
        }
    }
    private static String sceneName(int n){
        if(n>=18)return "single-column-"+new String[]{"empty","partial","full"}[n-18];
        if(n<12)return new String[]{"empty","partial","full"}[n/4]+"-"+new String[]{"south","north","east","west"}[n%4]+"-day-ao";
        return new String[]{"partial-day-no-ao","partial-night-artificial-ao","empty-night-artificial-ao","full-night-artificial-ao","controller-removed","controller-restored"}[n-12];
    }
    private static void scene(int n){
        var mc=Minecraft.getInstance();mc.options.ambientOcclusion().set(n!=12);
        mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();
            level.setDayTime(n>=13 && n<=15?18000:6000);
            if(n>=18){
                var singlePos=new BlockPos(3,80,0);var single=(TankControllerBlockEntity)level.getBlockEntity(singlePos);
                single.getTank().setFluid(n==18?FluidStack.EMPTY:new FluidStack(Fluids.WATER,n==19?16000:single.getTank().getCapacity()));
                level.sendBlockUpdated(singlePos,single.getBlockState(),single.getBlockState(),3);
            }
            if(n==16)level.setBlockAndUpdate(CORE,Blocks.AIR.defaultBlockState());
            else {
                if(!(level.getBlockEntity(CORE) instanceof TankControllerBlockEntity))level.setBlockAndUpdate(CORE,PressureContent.TANK_CONTROLLER.get().defaultBlockState());
                var core=(TankControllerBlockEntity)level.getBlockEntity(CORE);
                int amount=n<12?(n/4==0?0:n/4==1?48000:core.getTank().getCapacity()):n==14?0:n==15?core.getTank().getCapacity():48000;
                core.getTank().setFluid(amount==0?FluidStack.EMPTY:new FluidStack(Fluids.WATER,amount));
                level.sendBlockUpdated(CORE,core.getBlockState(),core.getBlockState(),3);
            }
            var player=level.getServer().getPlayerList().getPlayer(mc.player.getUUID());
            int view=n<12?n%4:0;
            switch(view){
                case 0->player.connection.teleport(2.5,82.8,10,180,5);
                case 1->player.connection.teleport(2.5,82.8,-10,0,5);
                case 2->player.connection.teleport(12,84,7,120,12);
                case 3->player.connection.teleport(-9,84,7,-60,12);
            }
        });
    }
    private static void setup(){
        var mc=Minecraft.getInstance();var level=mc.getSingleplayerServer().overworld();
        level.setWeatherParameters(100000,0,false,false);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
        for(int x=-12;x<=18;x++)for(int z=-12;z<=13;z++){
            level.setBlockAndUpdate(new BlockPos(x,80,z),Blocks.SMOOTH_STONE.defaultBlockState());
            for(int y=81;y<=88;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(CORE,PressureContent.TANK_CONTROLLER.get().defaultBlockState());
        for(int y=81;y<=83;y++){
            if(y>81)level.setBlockAndUpdate(new BlockPos(0,y,0),PressureContent.TANK_BLOCK.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(1,y,0),PressureContent.TANK_INDICATOR.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(5,y,0),PressureContent.TANK_INDICATOR.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(6,y,0),PressureContent.TANK_BLOCK.get().defaultBlockState());
        }
        level.setBlockAndUpdate(new BlockPos(3,81,0),PressureContent.TANK_INDICATOR.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(3,80,0),PressureContent.TANK_CONTROLLER.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(3,79,0),PressureContent.TANK_BLOCK.get().defaultBlockState());
        for(int x:new int[]{0,2,4,6})for(int z:new int[]{-2,2})level.setBlockAndUpdate(new BlockPos(x,80,z),Blocks.SEA_LANTERN.defaultBlockState());
        level.setBlockAndUpdate(GUI_CORE,PressureContent.TANK_CONTROLLER.get().defaultBlockState());level.setBlockAndUpdate(GUI_CORE.south(),PressureContent.TANK_BLOCK.get().defaultBlockState());
        for(int x=1;x<=6;x++)level.setBlockAndUpdate(GUI_CORE.east(x),PressureContent.TANK_FLUID_OUTPUT.get().defaultBlockState());
        var player=level.getServer().getPlayerList().getPlayer(mc.player.getUUID());player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    private static void openGui(int outputs){
        var mc=Minecraft.getInstance();mc.player.closeContainer();
        mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();
            for(int x=1;x<=6;x++)level.setBlockAndUpdate(GUI_CORE.east(x).north(),x<=outputs?PressureContent.WATER_SOURCE.get().defaultBlockState():Blocks.AIR.defaultBlockState());
            var player=level.getServer().getPlayerList().getPlayer(mc.player.getUUID());player.connection.teleport(2,82,8,180,30);
            var core=(TankControllerBlockEntity)level.getBlockEntity(GUI_CORE);
            core.getTank().setFluid(new FluidStack(Fluids.WATER,16000));
            level.sendBlockUpdated(GUI_CORE,core.getBlockState(),core.getBlockState(),3);
            NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new TankMenu(id,inv,GUI_CORE),Component.translatable("pressure.gui.tank.title")),
                    data->{data.writeBlockPos(GUI_CORE);data.writeNbt(core.getUpdateTag());});
        });
    }
    private static void capture(String name){
        var mc=Minecraft.getInstance();
        try{Path dir=mc.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(dir);
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(dir.resolve(name));}
            System.out.println("PRESSURE_REVISION_SCREENSHOT "+name);
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
