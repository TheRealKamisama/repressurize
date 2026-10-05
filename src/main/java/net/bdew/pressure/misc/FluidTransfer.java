package net.bdew.pressure.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public final class FluidTransfer {
    private FluidTransfer() {}

    public static IFluidHandler getHandler(Level level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return null;
        scala.Option<IFluidHandler> handler = net.bdew.lib.capabilities.helpers.FluidHelper$.MODULE$
                .getFluidHandler(level, pos, side);
        return handler.isDefined() ? handler.get() : null;
    }

    public static int transfer(IFluidHandler from, IFluidHandler to, int maxAmount) {
        if (from == null || to == null || maxAmount <= 0) return 0;
        FluidStack moved = net.bdew.lib.capabilities.helpers.FluidHelper$.MODULE$
                .pushFluid(from, to, FluidAction.EXECUTE, maxAmount);
        return moved.isEmpty() ? 0 : moved.getAmount();
    }
}
