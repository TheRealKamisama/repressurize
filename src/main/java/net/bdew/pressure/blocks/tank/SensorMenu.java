package net.bdew.pressure.blocks.tank;

import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidUtil;

public final class SensorMenu extends AbstractContainerMenu {
    public final TankModuleBlockEntity sensor;
    private final BlockPos pos;
    public SensorMenu(int id,Inventory inventory,FriendlyByteBuf data) {
        this(id,inventory,data.readBlockPos());var tag=data.readNbt();if(tag!=null)sensor.handleUpdateTag(tag);
    }
    public SensorMenu(int id,Inventory inventory,BlockPos pos) {
        super(PressureContent.SENSOR_MENU.get(),id);this.pos=pos;
        sensor=(TankModuleBlockEntity)inventory.player.level().getBlockEntity(pos);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,93+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,151));
    }
    @Override public boolean stillValid(Player player) {
        return sensor!=null && !sensor.isRemoved() && player.level().getBlockEntity(pos)==sensor && player.distanceToSqr(Vec3.atCenterOf(pos))<=64;
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(player.level().isClientSide || !stillValid(player))return false;
        if(button==0)sensor.cycleSensorMode();
        else if(button==1)switch(sensor.getSensorMode()) {
            case TANK_FILL -> sensor.cycleFillThreshold();
            case OUTPUT_FLOW -> sensor.cycleSensorOutput();
            case FLUID_MATCH -> sensor.getCapability(PressureAPI.FILTERABLE).ifPresent(filter->{
                if(getCarried().isEmpty())filter.clearFluidFilter();
                else FluidUtil.getFluidContained(getCarried()).ifPresent(fluid->filter.setFluidFilter(fluid.getFluid()));
            });
            default -> { return false; }
        }
        else return false;
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();var copy=stack.copy();
        if(index<27 ? !moveItemStackTo(stack,27,36,false) : !moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
}
