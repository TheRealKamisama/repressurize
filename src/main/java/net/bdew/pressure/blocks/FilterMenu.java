package net.bdew.pressure.blocks;

import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.api.properties.IFilterable;
import net.bdew.pressure.compat.EnderIOFilter;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** A server-authorized target and face bound to the open fluid-selection menu. */
public final class FilterMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Direction side;
    private final BlockEntity target;
    public FilterMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,data.readBlockPos(),data.readEnum(Direction.class));}
    public FilterMenu(int id,Inventory inventory,BlockPos pos,Direction side) {
        super(PressureContent.FILTER_MENU.get(),id);this.pos=pos;this.side=side;target=inventory.player.level().getBlockEntity(pos);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,88+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,146));
    }
    public static IFilterable filter(BlockEntity target,Direction side) {
        if(target==null)return null;
        return target.getCapability(PressureAPI.FILTERABLE,side).orElseGet(()->EnderIOFilter.find(target));
    }
    @Override public boolean stillValid(Player player) {
        return target!=null && !target.isRemoved() && player.level().getBlockEntity(pos)==target && player.distanceToSqr(Vec3.atCenterOf(pos))<=64;
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        if(player.level().isClientSide || !stillValid(player))return false;
        var filter=filter(target,side);if(filter==null)return false;
        if(id==0)filter.clearFluidFilter();
        else {
            Fluid fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(id-1);
            if(fluid==Fluids.EMPTY || !ForgeRegistries.FLUIDS.containsValue(fluid))return false;
            filter.setFluidFilter(fluid);
        }
        player.closeContainer();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();var copy=stack.copy();
        if(index<27 ? !moveItemStackTo(stack,27,36,false) : !moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
}
