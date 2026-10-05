package net.bdew.pressure.items;

import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.api.properties.IFilterable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Fluid filter configurator; filter selection is stored by filterable machines. */
public final class ConfiguratorItem extends Item {
    public ConfiguratorItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity target = context.getLevel().getBlockEntity(context.getClickedPos());
        if (target == null) return InteractionResult.PASS;
        IFilterable filterable = net.bdew.pressure.blocks.FilterMenu.filter(target, context.getClickedFace());
        if (filterable == null) return InteractionResult.PASS;

        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!context.getLevel().mayInteract(player, context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        var sample = player.getOffhandItem();
        if (sample.isEmpty() && player.isCrouching()) {
            filterable.clearFluidFilter();
            player.displayClientMessage(Component.translatable("pressure.label.filter.unset"), true);
        } else {
            FluidStack fluid = FluidUtil.getFluidContained(sample).orElse(FluidStack.EMPTY);
            if (fluid.isEmpty()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, menuPlayer) -> new net.bdew.pressure.blocks.FilterMenu(id, inventory, context.getClickedPos(), context.getClickedFace()),
                            getDescription()), data -> { data.writeBlockPos(context.getClickedPos()); data.writeEnum(context.getClickedFace()); });
                return InteractionResult.CONSUME;
            }
            filterable.setFluidFilter(fluid.getFluid());
            player.displayClientMessage(Component.translatable("pressure.label.filter.set", fluid.getDisplayName()), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) { return useOn(context); }
}
