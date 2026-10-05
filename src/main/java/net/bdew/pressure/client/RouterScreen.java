package net.bdew.pressure.client;

import net.bdew.pressure.blocks.router.RouterMenu;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.blocks.router.RouterSideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Compact six-face editor for router routing priority and redstone behavior. */
public final class RouterScreen extends AbstractContainerScreen<RouterMenu> {
    private static final String[] MODE_LABELS = {"OFF", "IN", "ACT", "LO", "MED", "HI"};
    private static final String[] REDSTONE_LABELS = {"ALW", "ON", "OFF", "NEV"};
    private final List<Button> modeButtons = new ArrayList<>(6);
    private final List<Button> redstoneButtons = new ArrayList<>(6);

    public RouterScreen(RouterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 201;
    }

    @Override
    protected void init() {
        super.init();
        modeButtons.clear();
        redstoneButtons.clear();
        for (int i = 0; i < 6; i++) {
            int index = i;
            int x = leftPos + 17 + i * 24;
            int mode = menu.sideMode(i).ordinal();
            Button modeButton = Button.builder(Component.literal(MODE_LABELS[mode]),
                            button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index))
                    .bounds(x, topPos + 41, 22, 22).build();
            modeButton.setTooltip(Tooltip.create(Component.translatable(menu.sideMode(i).translationKey())));
            addRenderableWidget(modeButton);
            modeButtons.add(modeButton);

            int redstone = menu.redstoneMode(i).ordinal();
            Button redstoneButton = Button.builder(Component.literal(REDSTONE_LABELS[redstone]),
                            button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 6 + index))
                    .bounds(x, topPos + 65, 22, 22).build();
            redstoneButton.setTooltip(Tooltip.create(Component.translatable(menu.redstoneMode(i).translationKey())));
            addRenderableWidget(redstoneButton);
            redstoneButtons.add(redstoneButton);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 6; i++) {
            RouterSideMode mode = menu.sideMode(i);
            modeButtons.get(i).setMessage(Component.literal(MODE_LABELS[mode.ordinal()]));
            modeButtons.get(i).setTooltip(Tooltip.create(Component.translatable(mode.translationKey())));
            RouterRedstoneMode redstone = menu.redstoneMode(i);
            redstoneButtons.get(i).setMessage(Component.literal(REDSTONE_LABELS[redstone.ordinal()]));
            redstoneButtons.get(i).setTooltip(Tooltip.create(Component.translatable(redstone.translationKey())));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            Component name = Component.translatable("pressure.direction." + direction.getName() + ".short");
            int textWidth = font.width(name);
            int centerX = leftPos + 28 + i * 24;
            graphics.drawString(font, name, centerX - textWidth / 2, topPos + 24, 0x404040, false);
        }
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
            graphics.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
            graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        }
        for (int i = 0; i < 6; i++) {
            var filter = menu.filter(i);
            if (filter != null) FluidGauge.draw(graphics, new net.minecraftforge.fluids.FluidStack(filter, 1),
                    leftPos + 17 + i * 24, topPos + 90, 16, 16);
        }
        graphics.drawString(font, playerInventoryTitle, leftPos + 8, topPos + 109, 0x404040, false);
    }

    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        renderBackground(graphics);
        super.render(graphics, x, y, partial);
        renderTooltip(graphics, x, y);
        for (int i = 0; i < 6; i++) if (isHovering(17 + i * 24, 90, 16, 16, x, y)) {
            var filter = menu.filter(i);
            List<Component> tip = new ArrayList<>();
            tip.add(filter == null ? Component.translatable("pressure.gui.tank.nofilter")
                    : new net.minecraftforge.fluids.FluidStack(filter, 1).getDisplayName());
            tip.add(Component.translatable("pressure.gui.tank.filter.tip1"));
            tip.add(Component.translatable("pressure.gui.tank.filter.tip2"));
            graphics.renderComponentTooltip(font, tip, x, y);
        }
        for (int i = 0; i < 6; i++) if (isHovering(17 + i * 24, 23, 22, 14, x, y))
            graphics.renderTooltip(font, Component.translatable("bdlib.multiblock.face." + Direction.values()[i].getName()), x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
    }
}
