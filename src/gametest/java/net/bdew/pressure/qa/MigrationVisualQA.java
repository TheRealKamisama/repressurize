package net.bdew.pressure.qa;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.blocks.tank.TankMenu;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Opt-in fresh-client evidence fixture. Compiled only in the gameTest source set, never shipped. */
@Mod.EventBusSubscriber(modid="pressureqa",value=Dist.CLIENT)
public final class MigrationVisualQA {
    private static int phase, ticks;
    private static final BlockPos CORE = new BlockPos(-1,81,-10);

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("pressure.migrationQa") || event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        if (phase==0 && mc.screen instanceof TitleScreen && ++ticks>40) {
            phase=1; ticks=0;
            mc.options.pauseOnLostFocus=false;
            mc.options.particles().set(net.minecraft.client.ParticleStatus.ALL);
            mc.createWorldOpenFlows().loadLevel(mc.screen,"Migration QA");
            return;
        }
        if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null) return;
        ticks++;
        if (phase==1 && ticks>80) {
            phase=2; ticks=0;
            mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())));
        } else if(phase==2 && ticks>120) {
            VisualModelAssertions.verify();
            capture("01-machines-all-facings.png"); phase=20; ticks=0; camera(-13,92,29,180,35);
        } else if(phase==20 && ticks>80) {
            capture("01b-powered-machines-all-facings.png"); phase=3; ticks=0; camera(25,89,24,180,30);
        } else if(phase==3 && ticks>80) {
            capture("02-pipe-rings.png"); phase=4; ticks=0; camera(3,84,-2,180,15);
        } else if(phase==4 && ticks>80) {
            capture("03-tank-frames.png"); phase=40; ticks=0; camera(3.5,82.4,-21,0,17);
        } else if(phase==40 && ticks>80) {
            capture("03a-unassembled-tank-frames.png"); phase=41; ticks=0; camera(3,84,-2,180,15);
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setBlockAndUpdate(CORE,Blocks.AIR.defaultBlockState()));
        } else if(phase==41 && ticks>80) {
            capture("03c-controller-removed.png");phase=42;ticks=0;
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();level.setBlockAndUpdate(CORE,PressureContent.TANK_CONTROLLER.get().defaultBlockState());
                var core=(TankControllerBlockEntity)level.getBlockEntity(CORE);
                core.getTank().fill(new FluidStack(Fluids.WATER,48000),FluidAction.EXECUTE);
                core.getCapability(net.bdew.pressure.api.PressureAPI.FILTERABLE).ifPresent(f->f.setFluidFilter(Fluids.WATER));
                core.getInventory().setStackInSlot(0,new ItemStack(Items.BUCKET));
            });
        } else if(phase==42 && ticks>80) {
            capture("03d-controller-restored.png");phase=43;ticks=0;camera(0.5,82.6,-6.5,180,30);
        } else if(phase==43 && ticks>80) {
            capture("03b-tank-overlay.png"); phase=5; ticks=0; mc.setScreen(new InventoryGallery());
            if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))JeiQaPlugin.verify();
        } else if(phase==5 && ticks>40) {
            capture("04-inventory-canisters.png"); phase=6; ticks=0; mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var core=(TankControllerBlockEntity)player.level().getBlockEntity(CORE);
                for(int x:new int[]{2,3,6})for(int y=81;y<=82;y++)player.level().setBlockAndUpdate(new BlockPos(x,y,-9),PressureContent.WATER_SOURCE.get().defaultBlockState());
                player.teleportTo(0,82,-8);
                NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new TankMenu(id,inv,CORE),
                        Component.translatable("pressure.gui.tank.title")),data->{data.writeBlockPos(CORE);data.writeNbt(core.getUpdateTag());});
            });
        } else if(phase==6 && ticks>60) {
            capture("05-tank-gui.png"); phase=7; ticks=0; openMenu("sensor");
        } else if(phase==7 && ticks>40) {
            capture("06-sensor-gui.png");phase=8;ticks=0;openMenu("filter");
        } else if(phase==8 && ticks>40) {
            capture("07-configurator-gui.png");phase=9;ticks=0;openMenu("router");
        } else if(phase==9 && ticks>40) {
            capture("08-router-gui.png");phase=30;ticks=0;mc.player.closeContainer();mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var nether=mc.getSingleplayerServer().getLevel(net.minecraft.world.level.Level.NETHER);
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                    nether.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
                    for(int y=101;y<=106;y++)nether.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                }
                player.teleportTo(nether,2,102,3,150,35);player.getAbilities().flying=true;player.onUpdateAbilities();
                var canister=new ItemStack(PressureContent.CANISTER.get());
                canister.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(h->h.fill(new FluidStack(Fluids.WATER,2500),FluidAction.EXECUTE));
                player.getInventory().selected=0;player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,canister);
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));player.inventoryMenu.broadcastChanges();
            });
        } else if(phase==30 && ticks>60 && mc.level.dimension()==net.minecraft.world.level.Level.NETHER && mc.screen==null
                && mc.level.getBlockState(new BlockPos(0,100,0)).is(Blocks.STONE)){
            BlockPos floor=new BlockPos(0,100,0);
            mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0,0.5,0),Direction.UP,floor,false));
            phase=31;ticks=0;
        } else if(phase==31 && ticks>3 && mc.screen==null){
            int amount=mc.player.getMainHandItem().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(h->h.getFluidInTank(0).getAmount()).orElse(-1);
            if(amount!=1500){if(ticks>100)throw new IllegalStateException("Nether client inventory did not synchronize");return;}
            if(!mc.level.getFluidState(new BlockPos(0,101,0)).isEmpty())throw new IllegalStateException("Nether water did not vaporize");
            capture("11-nether-vaporization.png");System.out.println("PRESSURE_NETHER_CLIENT_QA_PASSED amount="+amount);
            phase=10;ticks=0;
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                player.teleportTo(mc.getSingleplayerServer().overworld(),25,84,24,180,15);player.getAbilities().flying=true;player.onUpdateAbilities();
                player.getInventory().selected=0;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(PressureContent.PIPE.get()));
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(PressureContent.PIPE.get()));
                player.inventoryMenu.broadcastChanges();
            });
        } else if(phase==10 && ticks>60) {
            capture("09-pipe-held-first-person.png");phase=11;ticks=0;
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
        } else if(phase==11 && ticks>40) {
            capture("10-pipe-held-third-person.png");phase=12;ticks=0;
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);camera(25.5,82,26,180,45);
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();
                var item=new net.minecraft.world.entity.item.ItemEntity(level,25.5,81.25,23.5,new ItemStack(PressureContent.PIPE.get()));
                item.setDeltaMovement(0,0,0);level.addFreshEntity(item);
            });
        } else if(phase==12 && ticks>60){
            capture("12-pipe-dropped.png");phase=13;mc.stop();
        }
    }

    private static void openMenu(String type) {
        Minecraft mc=Minecraft.getInstance();mc.player.closeContainer();
        mc.getSingleplayerServer().execute(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            BlockPos pos=type.equals("sensor")?new BlockPos(8,81,-10):type.equals("router")?new BlockPos(0,81,-8):CORE;
            player.teleportTo(pos.getX()+1,pos.getY()+1,pos.getZ()+2);
            NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inventory,p)->switch(type) {
                case "sensor" -> new net.bdew.pressure.blocks.tank.SensorMenu(id,inventory,pos);
                case "router" -> new net.bdew.pressure.blocks.router.RouterMenu(id,inventory,pos);
                default -> new net.bdew.pressure.blocks.FilterMenu(id,inventory,pos,Direction.SOUTH);
            },Component.translatable(type.equals("sensor")?"pressure.gui.sensor.title":type.equals("router")?"pressure.gui.router.title":"item.pressure.configurator")),data->{
                data.writeBlockPos(pos);
                if(type.equals("sensor"))data.writeNbt(player.level().getBlockEntity(pos).getUpdateTag());
                if(type.equals("filter"))data.writeEnum(Direction.SOUTH);
            });
        });
    }

    private static void capture(String name) {
        Minecraft mc=Minecraft.getInstance();
        try {
            var dir=mc.gameDirectory.toPath().resolve("screenshots"); Files.createDirectories(dir);
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(dir.resolve(name)); }
            System.out.println("MIGRATION_QA_SCREENSHOT "+name);
        } catch(Exception e) { throw new IllegalStateException("Failed visual QA capture",e); }
    }
    private static void camera(double x,double y,double z,float yaw,float pitch) {
        var mc=Minecraft.getInstance();
        mc.getSingleplayerServer().execute(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            player.connection.teleport(x,y,z,yaw,pitch);
        });
    }
    private static void setup(ServerPlayer player) {
        var level=player.serverLevel();
        level.setDayTime(1000); level.setWeatherParameters(100000,0,false,false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,level.getServer());
        for(int x=-24;x<37;x++) for(int z=-17;z<25;z++) {
            level.setBlockAndUpdate(new BlockPos(x,80,z),Blocks.SMOOTH_STONE.defaultBlockState());
            for(int y=81;y<91;y++) level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
        }
        Block[] machines={PressureContent.INPUT.get(),PressureContent.OUTPUT.get(),PressureContent.DRAIN.get(),PressureContent.SLUICE.get(),
                PressureContent.CHECK_VALVE.get(),PressureContent.PIPE_SENSOR.get(),PressureContent.TANK_CONTROLLER.get(),PressureContent.PUMP.get()};
        int row=0;
        for(Direction facing:Direction.values()) {
            for(int i=0;i<machines.length;i++) {
                var state=machines[i].defaultBlockState();
                if(state.hasProperty(BlockStateProperties.FACING)) state=state.setValue(BlockStateProperties.FACING,facing);
                BlockPos pos=new BlockPos(i*2,81,row*3);
                level.setBlockAndUpdate(pos,state);
                BlockPos powered=pos.west(20);
                level.setBlockAndUpdate(powered,state);
                if(state.hasProperty(BlockStateProperties.POWERED)) level.setBlockAndUpdate(powered,state.setValue(BlockStateProperties.POWERED,true));
                var signal=state.getProperties().stream().filter(p->p.getName().equals("signal")).findFirst();
                if(signal.isPresent()) level.setBlockAndUpdate(powered,state.setValue((net.minecraft.world.level.block.state.properties.BooleanProperty)signal.get(),true));
            }
            row++;
        }
        java.util.Set<BlockPos> pipePositions=new java.util.HashSet<>();
        for(int i=0;i<5;i++) {
            for(BlockPos p:List.of(new BlockPos(20+i,81,1),new BlockPos(20+i,81,5),new BlockPos(20,81,1+i),new BlockPos(24,81,1+i),
                    new BlockPos(20+i,82,10),new BlockPos(20+i,86,10),new BlockPos(20,82+i,10),new BlockPos(24,82+i,10),
                    new BlockPos(29,82+i,10),new BlockPos(29,82+i,14),new BlockPos(29,82,10+i),new BlockPos(29,86,10+i)))
                pipePositions.add(p);
        }
        for(BlockPos p:pipePositions) level.setBlockAndUpdate(p,PressureContent.PIPE.get().defaultBlockState());
        // setBlock has no player placement context; finish all connection states after assembling the fixture.
        for(BlockPos p:pipePositions) {
            var state=level.getBlockState(p);
            level.setBlockAndUpdate(p,state.updateShape(Direction.NORTH,level.getBlockState(p.north()),level,p,p.north()));
        }
        Block[] modules={PressureContent.TANK_BLOCK.get(),PressureContent.TANK_INDICATOR.get(),PressureContent.TANK_FLUID_OUTPUT.get(),
                PressureContent.TANK_FLUID_OUTPUT_AUTO.get(),PressureContent.TANK_FLUID_INPUT.get(),PressureContent.TANK_FLUID_ACCESS.get(),PressureContent.TANK_PRESSURE_OUTPUT.get(),PressureContent.TANK_PRESSURE_INPUT.get()};
        for(int x=0;x<modules.length;x++) for(int y=81;y<=82;y++) {
            level.setBlockAndUpdate(new BlockPos(x,y,-10),modules[x].defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x,y,-14),modules[x].defaultBlockState());
        }
        level.setBlockAndUpdate(new BlockPos(8,81,-10),PressureContent.TANK_SENSOR.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(0,81,-8),PressureContent.ROUTER.get().defaultBlockState());
        var router=(net.bdew.pressure.blockentity.RouterBlockEntity)level.getBlockEntity(new BlockPos(0,81,-8));
        for(Direction side:Direction.values()) for(int n=0;n<side.ordinal();n++)router.cycleSideMode(side);
        level.setBlockAndUpdate(CORE,PressureContent.TANK_CONTROLLER.get().defaultBlockState());
        var core=(TankControllerBlockEntity)level.getBlockEntity(CORE);
        core.getTank().fill(new FluidStack(Fluids.WATER,48000),FluidAction.EXECUTE);
        core.getCapability(net.bdew.pressure.api.PressureAPI.FILTERABLE).ifPresent(filter->filter.setFluidFilter(Fluids.WATER));
        core.getInventory().setStackInSlot(0,new ItemStack(Items.BUCKET));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.getAbilities().flying=true;player.onUpdateAbilities();
        player.connection.teleport(7,92,29,180,35);
    }

    private static final class InventoryGallery extends Screen {
        private final List<ItemStack> stacks=new ArrayList<>();
        InventoryGallery() {
            super(Component.literal("Migration visual QA"));
            PressureContent.ITEMS.getEntries().forEach(item->stacks.add(new ItemStack(item.get())));
            for(var fluid:List.of(Fluids.WATER,Fluids.LAVA)) for(int amount:new int[]{1,16000,32000,64000}) {
                var stack=new ItemStack(PressureContent.CANISTER.get());
                stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(h->h.fill(new FluidStack(fluid,amount),FluidAction.EXECUTE));
                stacks.add(stack);
            }
            var modded=net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValues().stream()
                    .filter(f->net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(f).getNamespace().equals("mekanism") && f.defaultFluidState().isSource()).findFirst();
            modded.ifPresent(fluid->{
                for(int amount:new int[]{16000,64000}){
                    var stack=new ItemStack(PressureContent.CANISTER.get());
                    stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(h->h.fill(new FluidStack(fluid,amount),FluidAction.EXECUTE));
                    stacks.add(stack);
                }
            });
        }
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
            g.fill(0,0,width,height,0xFFB0B0B0);
            g.drawString(font,"Current item models / empty, partial and full canisters",8,6,0xFF101010,false);
            for(int i=0;i<stacks.size();i++) {
                int x=10+(i%10)*31,y=25+(i/10)*32;
                g.fill(x-1,y-1,x+25,y+25,0xFF404040);
                g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(1.5F,1.5F,1.5F);
                g.renderItem(stacks.get(i),0,0);g.renderItemDecorations(font,stacks.get(i),0,0);g.pose().popPose();
            }
        }
    }
}
