package net.bdew.pressure.items;

import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.misc.FluidTransfer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

public final class HandPumpItem extends Item {
    // Durability already makes this item non-stackable. Do not call stacksTo here.
    public HandPumpItem(Properties properties) { super(properties); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(player.getItemInHand(hand));
        InteractionResult result = useOn(new UseOnContext(player, hand, hit));
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) { return useOn(context); }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || player.isCrouching()) return InteractionResult.PASS;
        var pos = context.getClickedPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;

        IFluidHandler source = FluidTransfer.getHandler(level, pos, context.getClickedFace());
        boolean worldSource = source == null;
        if (worldSource) {
            if (!level.getFluidState(pos).isSource() || !(level.getBlockState(pos).getBlock() instanceof BucketPickup))
                return InteractionResult.PASS;
            FluidStack water = new FluidStack(level.getFluidState(pos).getType(), 1000);
            source = new IFluidHandler() {
                @Override public int getTanks() { return 1; }
                @Override public FluidStack getFluidInTank(int tank) { return water.copy(); }
                @Override public int getTankCapacity(int tank) { return 1000; }
                @Override public boolean isFluidValid(int tank, FluidStack fluid) { return false; }
                @Override public int fill(FluidStack fluid, FluidAction action) { return 0; }
                @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
                    return fluid.isFluidEqual(water) ? drain(fluid.getAmount(), action) : FluidStack.EMPTY;
                }
                @Override public FluidStack drain(int amount, FluidAction action) {
                    if (amount < 1000 || !level.getFluidState(pos).isSource() || level.getFluidState(pos).getType() != water.getFluid())
                        return FluidStack.EMPTY;
                    if (action.simulate()) return water.copy();
                    var state = level.getBlockState(pos);
                    if (state.getBlock() instanceof BucketPickup pickup)
                        return FluidUtil.getFluidContained(pickup.pickupBlock(level, pos, state)).orElse(FluidStack.EMPTY);
                    return FluidStack.EMPTY;
                }
            };
        }
        int max = worldSource ? 1000 : PressureConfig.HAND_PUMP_MAX_DRAIN.get();
        InvWrapper inventory = new InvWrapper(player.getInventory());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.isEmpty() || candidate == context.getItemInHand()) continue;
            var simulated = FluidUtil.tryFillContainer(candidate, source, max, player, false);
            if (!simulated.isSuccess()) continue;
            if (candidate.getCount() > 1 && !ItemHandlerHelper.insertItemStacked(inventory, simulated.getResult(), true).isEmpty()) continue;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            var result = FluidUtil.tryFillContainer(candidate, source, max, player, true);
            if (!result.isSuccess()) return InteractionResult.FAIL;
            if (candidate.getCount() == 1) player.getInventory().setItem(slot, result.getResult());
            else {
                candidate.shrink(1);
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, result.getResult(), false);
                if (!remainder.isEmpty()) player.drop(remainder, false);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            if (!player.getAbilities().instabuild)
                context.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GENERIC_DRINK, player.getSoundSource(), 0.5F, 1F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
