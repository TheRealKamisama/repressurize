package net.bdew.pressure.items;

import net.bdew.pressure.api.IPressureEject;
import net.bdew.pressure.api.IPressureInject;
import net.bdew.pressure.api.IPressureNode;
import net.bdew.pressure.api.PressureAPI;
import net.bdew.pressure.config.PressureConfig;
import net.bdew.pressure.pressurenet.PressureGraph;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;

/** Diagnostic tool for inspecting a block's Pressure Pipes network connections. */
public final class DebuggerItem extends Item {
    public DebuggerItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var entity = level.getBlockEntity(pos);
        long start = System.nanoTime();
        var scan = PressureGraph.scanConnectedBlocks(level, pos, context.getClickedFace());
        var connections = PressureGraph.connectionsAt(level, pos);
        long elapsedMicros = (System.nanoTime() - start) / 1_000;

        String blockName = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        String connectionNames = connections.stream().map(Direction::getName).collect(Collectors.joining(", "));
        String role = (entity instanceof IPressureInject ? "injector " : "")
                + (entity instanceof IPressureEject ? "ejector" : "");
        if (role.isBlank()) role = entity instanceof IPressureNode ? "pressure node" : "not a pressure node";
        boolean fluidHandler = entity != null && entity.getCapability(ForgeCapabilities.FLUID_HANDLER,
                context.getClickedFace()).isPresent();

        var player = context.getPlayer();
        if (player != null) {
            player.displayClientMessage(Component.literal("Pressure debug: " + blockName).withStyle(ChatFormatting.GOLD), false);
            player.displayClientMessage(Component.literal("Role: " + role + "; fluid handler: " + fluidHandler), false);
            player.displayClientMessage(Component.literal("Ins: " + scan.inputs()), false);
            player.displayClientMessage(Component.literal("Outs: " + scan.outputs()), false);
            for (var visited : scan.traversed())
                player.displayClientMessage(Component.literal(" * " + visited.toShortString() + ": ")
                        .append(level.getBlockState(visited).getBlock().getName()), false);
            player.displayClientMessage(Component.literal("Pipe connections: "
                    + (connectionNames.isEmpty() ? "none" : connectionNames)
                    + " (" + elapsedMicros + " μs)"), false);
            player.displayClientMessage(Component.literal("Scanned " + scan.traversed().size() + " blocks."), false);
            player.displayClientMessage(Component.literal("Fluid transfer limit: "
                    + PressureConfig.FLUID_TRANSFER_PER_TICK.get() + " mB/t; helper: "
                    + (PressureAPI.HELPER == null ? "unavailable" : "ready")), false);
        }
        return InteractionResult.CONSUME;
    }
}
