package net.bdew.pressure.client;

import net.bdew.pressure.blocks.tank.TankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Legacy layout: gauge, container input/output, ghost filter and six output channels. */
public final class TankScreen extends AbstractContainerScreen<TankMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("pressure", "textures/gui/tank.png");
    private static final int[] COLORS = {0xFFD84B4B, 0xFF4BD84B, 0xFF4B6BD8, 0xFFD8D84B, 0xFF4BD8D8, 0xFFC04BD8};
    private final Button[] modes = new Button[6];
    // The texture's 18-pixel slot borders start at (43,42); (44,43) is the item inset.
    private static final int OUTPUT_X = 43, OUTPUT_Y = 42, OUTPUT_SIZE = 18, OUTPUT_PITCH = 21, BUTTON_Y = 63;
    private static int outputX(int slot) { return OUTPUT_X + OUTPUT_PITCH * slot; }

    public TankScreen(TankMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            final int slot = i;
            modes[i] = addRenderableWidget(Button.builder(Component.empty(), button -> {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, slot);
            }).bounds(leftPos + outputX(i), topPos + BUTTON_Y, OUTPUT_SIZE, OUTPUT_SIZE).build());
        }
    }

    @Override protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 6; i++) {
            var output = menu.controller.getOutput(i);
            modes[i].visible = output != null;
            if (output != null) modes[i].setMessage(Component.literal(switch (output.mode) {
                case ALWAYS -> "A"; case NEVER -> "X"; case RS_ON -> "+"; case RS_OFF -> "−";
            }));
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        var tank = menu.controller.getTank();
        int height = (int) Math.min(58, 58L * tank.getFluidAmount() / Math.max(1, tank.getCapacity()));
        FluidGauge.draw(graphics, tank.getFluid(), leftPos + 8, topPos + 77 - height, 16, height);
        graphics.blit(new ResourceLocation("pressure", "textures/gui/widgets.png"), leftPos + 8, topPos + 19, 0, 0, 16, 58);
        var filter = menu.controller.getFluidFilter();
        if (filter != null) FluidGauge.draw(graphics, new FluidStack(filter, 1000), leftPos + 149, topPos + 19, 16, 16);
        for (int i = 0; i < 6; i++) {
            int x = leftPos + outputX(i), y = topPos + OUTPUT_Y;
            graphics.fill(x, y, x + OUTPUT_SIZE, y + OUTPUT_SIZE, COLORS[i]);
            graphics.fill(x + 1, y + 1, x + OUTPUT_SIZE - 1, y + OUTPUT_SIZE - 1, 0xFF303030);
            var output = menu.controller.getOutput(i);
            if (output == null) graphics.drawCenteredString(font, "×", x + 9, y + 4, COLORS[i]);
            else if (minecraft.level != null) graphics.renderItem(minecraft.level.getBlockState(output.pos.relative(output.side))
                    .getBlock().asItem().getDefaultInstance(), x + 1, y + 1);
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        List<Component> tip = new ArrayList<>();
        if (isHovering(8, 19, 16, 58, mouseX, mouseY)) {
            var tank = menu.controller.getTank();
            tip.add(tank.isEmpty() ? Component.translatable("pressure.canister.empty") : tank.getFluid().getDisplayName());
            tip.add(Component.literal(tank.getFluidAmount() + " / " + tank.getCapacity() + " mB"));
        } else if (isHovering(149, 19, 16, 16, mouseX, mouseY)) {
            var filter = menu.controller.getFluidFilter();
            tip.add(filter == null ? Component.translatable("pressure.gui.tank.nofilter")
                    : Component.translatable("pressure.gui.tank.filter", new FluidStack(filter, 1).getDisplayName()));
            tip.add(Component.translatable("pressure.gui.tank.filter.tip1"));
            tip.add(Component.translatable("pressure.gui.tank.filter.tip2"));
        } else for (int i = 0; i < 6; i++) if (isHovering(outputX(i), OUTPUT_Y, OUTPUT_SIZE, OUTPUT_SIZE, mouseX, mouseY)
                || modes[i].visible && isHovering(outputX(i), BUTTON_Y, OUTPUT_SIZE, OUTPUT_SIZE, mouseX, mouseY)) {
            tip.add(Component.translatable("pressure.output." + i));
            var output = menu.controller.getOutput(i);
            if (output == null) tip.add(Component.translatable("bdlib.multiblock.disabled"));
            else {
                if (minecraft.level != null) tip.add(minecraft.level.getBlockState(output.pos.relative(output.side)).getBlock().getName());
                tip.add(Component.literal(output.pos.toShortString() + " / " + output.side.getName()));
                tip.add(Component.translatable(output.mode.translationKey()));
                tip.add(Component.literal(String.format(java.util.Locale.ROOT, "%.1f mB/t", output.average)));
            }
        }
        if (!tip.isEmpty()) graphics.renderComponentTooltip(font, tip, mouseX, mouseY);
    }
}
