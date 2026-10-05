package net.bdew.pressure.items;

import net.bdew.pressure.config.PressureContent;
import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.misc.FluidTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class CanisterItem extends Item {
    private static int capacity() { return PressureConfig.CANISTER_CAPACITY.get(); }

    public CanisterItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        IFluidHandlerItem canister = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (canister == null) return InteractionResult.PASS;
        var player = context.getPlayer();
        if (player == null || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack)
                || !level.mayInteract(player, context.getClickedPos())) return InteractionResult.FAIL;

        var target = FluidTransfer.getHandler(level, context.getClickedPos(), context.getClickedFace());
        if (target != null) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            FluidStack contained = canister.drain(1, FluidAction.SIMULATE);
            int moved;
            if (contained.isEmpty()) {
                FluidStack available = target.drain(PressureConfig.CANISTER_MAX_POUR.get(), FluidAction.SIMULATE);
                if (available.isEmpty()) return InteractionResult.PASS;
                moved = FluidTransfer.transfer(target, canister, PressureConfig.CANISTER_MAX_POUR.get());
            } else {
                moved = FluidTransfer.transfer(canister, target, PressureConfig.CANISTER_MAX_POUR.get());
            }
            if (moved > 0) {
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                return InteractionResult.CONSUME;
            }
        }

        if (canister.drain(FluidType.BUCKET_VOLUME, FluidAction.SIMULATE).getAmount() == FluidType.BUCKET_VOLUME) {
            BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
            FluidStack fluid = canister.drain(FluidType.BUCKET_VOLUME, FluidAction.SIMULATE);
            // The legacy canister only places into air, and consumes fluid in creative too.
            if (!level.isOutsideBuildHeight(targetPos) && level.getWorldBorder().isWithinBounds(targetPos)
                    && level.isEmptyBlock(targetPos) && level.mayInteract(player, targetPos)
                    && player.mayUseItemAt(targetPos, context.getClickedFace(), stack)
                    && fluid.getFluid().getFluidType().canBePlacedInLevel(level, targetPos, fluid)) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                boolean waterVaporizes = fluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                        && fluid.getFluid().getFluidType().isVaporizedOnPlacement(level, targetPos, fluid);
                // Forge drains a COPY, including when water vaporizes in the Nether.
                var result = FluidUtil.tryPlaceFluid(player, level, context.getHand(), targetPos, stack, fluid);
                if (result.isSuccess()) {
                    player.setItemInHand(context.getHand(), result.getResult());
                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();
                    if (waterVaporizes && level instanceof net.minecraft.server.level.ServerLevel server) {
                        // Forge's callback uses client-only addParticle and excludes the user
                        // from its sound broadcast. Supply those effects after server success.
                        server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 8, 0.5, 0.5, 0.5, 0);
                        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && serverPlayer.connection != null)
                            serverPlayer.playNotifySound(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
                    }
                    return InteractionResult.CONSUME;
                }
            }
        }

        if (canister.drain(1, FluidAction.SIMULATE).isEmpty() && level.getFluidState(context.getClickedPos()).isSource()) {
            var sourceFluid = level.getFluidState(context.getClickedPos()).getType();
            FluidStack source = new FluidStack(sourceFluid, FluidType.BUCKET_VOLUME);
            if (canister.fill(source, FluidAction.SIMULATE) == FluidType.BUCKET_VOLUME) {
                if (!level.isClientSide) {
                    var state = level.getBlockState(context.getClickedPos());
                    if (!(state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup)) return InteractionResult.PASS;
                    var picked = FluidUtil.getFluidContained(pickup.pickupBlock(level, context.getClickedPos(), state)).orElse(FluidStack.EMPTY);
                    if (picked.isEmpty()) return InteractionResult.PASS;
                    canister.fill(picked, FluidAction.EXECUTE);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = new CanisterFluidHandler(stack).contents();
        return fluid.isEmpty() ? super.getName(stack) : super.getName(stack).copy().append(" - ").append(fluid.getDisplayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = new CanisterFluidHandler(stack).contents();
        if (fluid.isEmpty()) tooltip.add(Component.translatable("pressure.canister.empty"));
        else tooltip.add(Component.literal(fluid.getAmount() + "/" + capacity() + " mB ").append(fluid.getDisplayName()));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        LazyOptional<IFluidHandlerItem> handler = LazyOptional.of(() -> new CanisterFluidHandler(stack));
        return new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
                return capability == ForgeCapabilities.FLUID_HANDLER_ITEM ? handler.cast() : LazyOptional.empty();
            }
        };
    }

    private static final class CanisterFluidHandler implements IFluidHandlerItem {
        private final ItemStack stack;

        private CanisterFluidHandler(ItemStack stack) { this.stack = stack; }

        private static FluidStack sourceVariant(FluidStack fluid) {
            if (fluid != null && fluid.getFluid() instanceof net.minecraft.world.level.material.FlowingFluid flowing
                    && flowing.getSource() != fluid.getFluid())
                return new FluidStack(flowing.getSource(), fluid.getAmount(), fluid.hasTag() ? fluid.getTag().copy() : null);
            return fluid;
        }

        private FluidStack contents() {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("FluidId") || tag.getInt("Amount") <= 0) return FluidStack.EMPTY;
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("FluidId"));
            var fluid = id == null ? null : ForgeRegistries.FLUIDS.getValue(id);
            return fluid == null ? FluidStack.EMPTY : sourceVariant(new FluidStack(fluid, tag.getInt("Amount"),
                    tag.contains("FluidTag") ? tag.getCompound("FluidTag").copy() : null));
        }

        private void setContents(FluidStack fluid) {
            fluid = sourceVariant(fluid);
            CompoundTag tag = stack.getOrCreateTag();
            if (fluid == null || fluid.isEmpty()) {
                tag.remove("FluidId");
                tag.remove("Amount");
                tag.remove("FluidTag");
                if (tag.isEmpty()) stack.setTag(null);
            } else {
                tag.putString("FluidId", ForgeRegistries.FLUIDS.getKey(fluid.getFluid()).toString());
                tag.putInt("Amount", fluid.getAmount());
                if (fluid.hasTag()) tag.put("FluidTag", fluid.getTag().copy());
                else tag.remove("FluidTag");
            }
        }

        @Override public ItemStack getContainer() { return stack; }
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? contents() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? capacity() : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) {
            fluid = sourceVariant(fluid);
            FluidStack current = contents();
            return stack.getCount() == 1 && tank == 0 && fluid != null && !fluid.isEmpty() &&
                    (current.isEmpty() || current.isFluidEqual(fluid));
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            resource = sourceVariant(resource);
            if (!isFluidValid(0, resource)) return 0;
            FluidStack current = contents();
            int accepted = Math.max(0, Math.min(capacity() - current.getAmount(), resource.getAmount()));
            if (accepted > 0 && action.execute()) {
                FluidStack updated = current.isEmpty() ? resource.copy() : current;
                updated.setAmount(current.getAmount() + accepted);
                setContents(updated);
            }
            return accepted;
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            resource = sourceVariant(resource);
            FluidStack current = contents();
            if (resource == null || !current.isFluidEqual(resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack current = contents();
            if (stack.getCount() != 1 || current.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
            int amount = Math.min(maxDrain, current.getAmount());
            FluidStack result = current.copy();
            result.setAmount(amount);
            if (action.execute()) {
                current.shrink(amount);
                setContents(current);
            }
            return result;
        }
    }
}
