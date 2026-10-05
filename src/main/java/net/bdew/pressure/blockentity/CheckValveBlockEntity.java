package net.bdew.pressure.blockentity;

import net.bdew.pressure.api.IPressureConnection;
import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.api.IPressureInject;
import net.bdew.pressure.blocks.CheckValveBlock;
import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

public final class CheckValveBlockEntity extends BlockEntity implements IPressureInject, IPressureEject {
    private IPressureConnection connection;

    public CheckValveBlockEntity(BlockPos pos, BlockState state) { super(PressureContent.CHECK_VALVE_BE.get(), pos, state); }
    private Direction facing() { return getBlockState().getValue(CheckValveBlock.FACING); }

    @Override
    public int eject(FluidStack resource, Direction sideTowardNetwork, boolean doEject) {
        if (getLevel() == null || getLevel().isClientSide || getBlockState().getValue(CheckValveBlock.SIGNAL)
                || sideTowardNetwork != facing().getOpposite()) return 0;
        if (connection == null) connection = PressureGraph.instance().recalculateConnectionInfo(this, facing());
        return connection.pushFluid(resource, doEject);
    }

    @Override public void invalidateConnection(Direction direction) { connection = null; }
    @Override public BlockPos pressureNodePos() { return getBlockPos(); }
    @Override public net.minecraft.world.level.Level pressureNodeWorld() { return getLevel(); }
}
