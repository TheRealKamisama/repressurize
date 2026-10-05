package net.bdew.pressure.blocks.tank;

import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.SlotItemHandler;

/** The filter is a ghost target: clicking it never consumes or moves the carried container. */
public final class TankMenu extends AbstractContainerMenu {
    public final TankControllerBlockEntity controller;
    private final BlockPos pos;

    public TankMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
        var tag = data.readNbt();
        if (tag != null) controller.handleUpdateTag(tag);
    }

    public TankMenu(int id, Inventory inventory, BlockPos pos) {
        super(PressureContent.TANK_MENU.get(), id);
        this.pos = pos;
        if (!(inventory.player.level().getBlockEntity(pos) instanceof TankControllerBlockEntity core))
            throw new IllegalStateException("Tank controller is missing at " + pos);
        this.controller = core;
        addSlot(new SlotItemHandler(core.getInventory(), 0, 44, 19));
        addSlot(new SlotItemHandler(core.getInventory(), 1, 80, 19) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, 149, 19) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot == 2) {
            if (!player.level().isClientSide && stillValid(player) && type == ClickType.PICKUP) {
                controller.getCapability(PressureAPI.FILTERABLE).ifPresent(filter -> {
                    if (getCarried().isEmpty()) filter.clearFluidFilter();
                    else FluidUtil.getFluidContained(getCarried()).ifPresent(fluid -> filter.setFluidFilter(fluid.getFluid()));
                });
            }
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || player.level().isClientSide || button < 0 || button >= 6) return false;
        controller.cycleOutputMode(button);
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return !controller.isRemoved() && player.level().getBlockEntity(pos) == controller
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index == 2) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 30) {
            if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 3, 30, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
