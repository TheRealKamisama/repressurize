package net.bdew.pressure.qa;

import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blocks.tank.TankMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Read-only views of the already-verified saved world, one peer at a time. */
@Mod.EventBusSubscriber(modid="pressureqa")
public final class PeerViewServer {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!Boolean.getBoolean("pressure.peerViewServer") || !(event.getEntity() instanceof ServerPlayer player))return;
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
        player.connection.teleport(NetworkQaServer.CORE.getX()+1.5,81,4.5,180,16);
    }
    public static void open(ServerPlayer player){
        if(!Boolean.getBoolean("pressure.peerViewServer") || player==null)return;
        var core=(TankControllerBlockEntity)player.level().getBlockEntity(NetworkQaServer.CORE);
        net.minecraftforge.network.NetworkHooks.openScreen(player,new net.minecraft.world.SimpleMenuProvider(
                (id,inv,p)->new TankMenu(id,inv,NetworkQaServer.CORE),net.minecraft.network.chat.Component.translatable("pressure.gui.tank.title")),
                data->{data.writeBlockPos(NetworkQaServer.CORE);data.writeNbt(core.getUpdateTag());});
    }
}
