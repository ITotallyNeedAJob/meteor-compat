package meteordevelopment.meteorclient.mixin;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectEndEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.util.Iterator;

/**
 * COMPAT 26.2: packet event source. Trimmed upstream port of ConnectionMixin:
 * receive (bundle-aware, cancellable), send/sent and connect events.
 * Proxy/exception branches dropped (those systems are not ported in compat).
 */
@Mixin(Connection.class)
public abstract class CompatPacketMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void compat$onHandlePacket(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Iterator<Packet<? super ClientGamePacketListener>> it = bundle.subPackets().iterator(); it.hasNext(); ) {
                if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(it.next(), (Connection) (Object) this)).isCancelled()) it.remove();
            }
        } else if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(packet, (Connection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"))
    private static void compat$onConnect(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        MeteorClient.EVENT_BUS.post(ServerConnectEndEvent.get(address));
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void compat$onSendHead(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(new PacketEvent.Send(packet, (Connection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("TAIL"))
    private void compat$onSendTail(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new PacketEvent.Sent(packet, (Connection) (Object) this));
    }
}
