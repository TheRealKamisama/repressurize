package net.bdew.pressure.client;

import net.bdew.pressure.blocks.TankSensorModuleBlock;
import net.bdew.pressure.blocks.tank.SensorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;

public final class SensorScreen extends AbstractContainerScreen<SensorMenu> {
    private Button mode,parameter;
    public SensorScreen(SensorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageHeight=175;inventoryLabelY=82;}
    private void send(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init() {
        super.init();
        mode=addRenderableWidget(Button.builder(Component.empty(),b->send(0)).bounds(leftPos+8,topPos+20,160,20).build());
        parameter=addRenderableWidget(Button.builder(Component.empty(),b->send(1)).bounds(leftPos+8,topPos+44,160,20).build());
    }
    @Override protected void containerTick() {
        super.containerTick();var sensor=menu.sensor;
        mode.setMessage(Component.translatable(sensor.getSensorMode().translationKey()));
        parameter.setMessage(switch(sensor.getSensorMode()) {
            case DISABLED -> Component.translatable("pressure.sensor.disabled");
            case TANK_FILL -> Component.translatable(sensor.getFillThreshold().translationKey());
            case OUTPUT_FLOW -> Component.translatable(sensor.getSensorOutput()<0?"pressure.sensor.output.any":"pressure.output."+sensor.getSensorOutput());
            case FLUID_MATCH -> sensor.getFluidFilter()==null?Component.translatable("pressure.sensor.fluid.tip.unset")
                    :new FluidStack(sensor.getFluidFilter(),1).getDisplayName();
        });
    }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {
        graphics.blit(new ResourceLocation("pressure","textures/gui/sensor.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        graphics.fill(leftPos+7,topPos+18,leftPos+169,topPos+80,0xFFC6C6C6);
        boolean on=menu.sensor.getBlockState().getValue(TankSensorModuleBlock.SIGNAL);
        graphics.drawString(font,Component.translatable(on?"pressure.sensor.on":"pressure.sensor.off"),leftPos+8,topPos+69,on?0xAA0000:0x404040,false);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) {
        renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);
    }
}
