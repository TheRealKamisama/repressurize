package net.bdew.pressure.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Shared by the canister decorator, tank screen and fluid filter icons. */
public final class FluidGauge {
    private FluidGauge() {}

    public static void draw(GuiGraphics graphics, FluidStack fluid, int x, int y, int width, int height) {
        if (fluid.isEmpty() || width <= 0 || height <= 0) return;
        var extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture = extensions.getStillTexture(fluid);
        if (texture == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color = extensions.getTintColor(fluid);
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, (color >>> 24) / 255F);
        for (int dy = 0; dy < height; dy += 16) for (int dx = 0; dx < width; dx += 16)
            graphics.blit(x + dx, y + dy, 0, Math.min(16, width - dx), Math.min(16, height - dy), sprite);
        graphics.setColor(1, 1, 1, 1);
    }
}
