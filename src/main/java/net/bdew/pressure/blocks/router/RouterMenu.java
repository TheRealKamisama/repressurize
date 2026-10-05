package net.bdew.pressure.blocks.router;

import net.bdew.pressure.blockentity.RouterBlockEntity;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

/** Synced router settings menu; fluid filters are set with the configurator item. */
public final class RouterMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final BlockPos pos;
    private final RouterBlockEntity router;
    private final ContainerData data;
    private final int[] syncedData = new int[12];

    public RouterMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos());
    }

    public RouterMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(PressureContent.ROUTER_MENU.get(), containerId);
        this.inventory = inventory;
        this.pos = pos;
        Level level = inventory.player.level();
        this.router = level.getBlockEntity(pos) instanceof RouterBlockEntity found ? found : null;
        this.data = new ContainerData() {
            @Override public int get(int index) {
                if (router == null || index < 0 || index >= 12) return 0;
                if (level.isClientSide) return syncedData[index];
                Direction side = Direction.values()[index % 6];
                return index < 6 ? router.getSideMode(side).ordinal() : router.getRedstoneMode(side).ordinal();
            }
            @Override public void set(int index, int value) {
                if (index >= 0 && index < syncedData.length) syncedData[index] = value;
            }
            @Override public int getCount() { return 12; }
        };
        addDataSlots(data);
        var filters = new net.minecraft.world.SimpleContainer(6);
        for (int i = 0; i < 6; i++) addSlot(new net.minecraft.world.inventory.Slot(filters, i, 17 + i * 24, 90) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.world.inventory.Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 119 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new net.minecraft.world.inventory.Slot(inventory, col, 8 + col * 18, 177));
    }

    public net.minecraft.world.level.material.Fluid filter(int index) {
        return router == null ? null : router.getSideFilter(Direction.values()[index]);
    }
    @Override public void clicked(int slot, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (slot >= 0 && slot < 6) {
            if (!player.level().isClientSide && stillValid(player) && type == net.minecraft.world.inventory.ClickType.PICKUP)
                router.getCapability(net.bdew.pressure.api.PressureAPI.FILTERABLE, Direction.values()[slot]).ifPresent(filter -> {
                    if (getCarried().isEmpty()) filter.clearFluidFilter();
                    else net.minecraftforge.fluids.FluidUtil.getFluidContained(getCarried()).ifPresent(fluid -> filter.setFluidFilter(fluid.getFluid()));
                });
            return;
        }
        super.clicked(slot, button, type, player);
    }

    public RouterSideMode sideMode(int sideIndex) {
        int value = data.get(sideIndex);
        return value >= 0 && value < RouterSideMode.values().length
                ? RouterSideMode.values()[value] : RouterSideMode.DISABLED;
    }

    public RouterRedstoneMode redstoneMode(int sideIndex) {
        int value = data.get(6 + sideIndex);
        return value >= 0 && value < RouterRedstoneMode.values().length
                ? RouterRedstoneMode.values()[value] : RouterRedstoneMode.ALWAYS;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (router == null || player.level().isClientSide || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64)
            return false;
        if (buttonId >= 0 && buttonId < 6) {
            router.cycleSideMode(Direction.values()[buttonId]);
            return true;
        }
        if (buttonId >= 6 && buttonId < 12) {
            router.cycleRedstoneMode(Direction.values()[buttonId - 6]);
            return true;
        }
        return false;
    }

    @Override public boolean stillValid(Player player) {
        return router != null && player.distanceToSqr(Vec3.atCenterOf(pos)) < 64
                && player.level().getBlockEntity(pos) == router;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 6 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem(); var copy = stack.copy();
        if (index < 33 ? !moveItemStackTo(stack, 33, 42, false) : !moveItemStackTo(stack, 6, 33, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    public Inventory inventory() { return inventory; }
    public BlockPos position() { return pos; }
}
