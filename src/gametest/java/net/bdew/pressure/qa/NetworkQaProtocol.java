package net.bdew.pressure.qa;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Test-only coordination. Actual item and menu actions use vanilla client packets. */
public final class NetworkQaProtocol {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("pressureqa","network"),()->"1","1"::equals,"1"::equals);
    public static void init(){
        CHANNEL.registerMessage(0,Stage.class,(message,buffer)->buffer.writeVarInt(message.value),buffer->new Stage(buffer.readVarInt()),(message,context)->{
            var ctx=context.get();ctx.enqueueWork(()->NetworkQaServer.stage(ctx.getSender(),message.value));ctx.setPacketHandled(true);
        });
    }
    public record Stage(int value){}
}
