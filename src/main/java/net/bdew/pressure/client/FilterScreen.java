package net.bdew.pressure.client;

import net.bdew.pressure.blocks.FilterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Searchable fluid picker with the legacy 27-cell layout and clear button. */
public final class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private EditBox search;
    private List<Fluid> fluids=List.of();
    private int offset;
    public FilterScreen(FilterMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageHeight=170;inventoryLabelY=77;}
    @Override protected void init() {
        super.init();
        search=new EditBox(font,leftPos+26,topPos+6,124,12,Component.translatable("itemGroup.search"));
        search.setBordered(false);search.setMaxLength(256);search.setResponder(value->refresh(value));addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("×"),button->send(0)).bounds(leftPos+152,topPos+4,16,16).build());
        refresh("");setInitialFocus(search);
    }
    private void refresh(String value) {
        String query=value.toLowerCase(Locale.ROOT);offset=0;
        fluids=ForgeRegistries.FLUIDS.getValues().stream().filter(f->f!=Fluids.EMPTY
                        && (!(f instanceof net.minecraft.world.level.material.FlowingFluid flowing) || flowing.getSource()==f))
                .filter(f->new FluidStack(f,1).getDisplayName().getString().toLowerCase(Locale.ROOT).contains(query)
                        || ForgeRegistries.FLUIDS.getKey(f).toString().contains(query))
                .sorted(Comparator.comparing(f->new FluidStack(f,1).getDisplayName().getString())).toList();
    }
    private void send(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        graphics.blit(new ResourceLocation("pressure","textures/gui/configurator.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        for(int i=0;i<27 && i+offset<fluids.size();i++)FluidGauge.draw(graphics,new FluidStack(fluids.get(i+offset),1),leftPos+8+i%9*18,topPos+22+i/9*18,16,16);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){graphics.drawString(font,playerInventoryTitle,8,77,0x404040,false);}
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0)for(int i=0;i<27 && i+offset<fluids.size();i++)if(isHovering(8+i%9*18,22+i/9*18,16,16,x,y)) {
            send(net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(fluids.get(i+offset))+1);return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        offset=Math.max(0,Math.min(Math.max(0,fluids.size()-27),offset-(int)Math.signum(delta)*9));return true;
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) {
        renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);
        for(int i=0;i<27 && i+offset<fluids.size();i++)if(isHovering(8+i%9*18,22+i/9*18,16,16,x,y))
            graphics.renderTooltip(font,new FluidStack(fluids.get(i+offset),1).getDisplayName(),x,y);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(search.isFocused() && key!=256)return search.keyPressed(key,scan,modifiers);
        return super.keyPressed(key,scan,modifiers);
    }
}
