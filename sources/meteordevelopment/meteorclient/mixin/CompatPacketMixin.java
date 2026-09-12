package meteordevelopment.meteorclient.mixin;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectEndEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.jetbrains.annotations.Nullable;
import io.netty.channel.ChannelFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.util.Iterator;

/**
 * COMPAT Stage 3: packet event source. Trimmed upstream port of
 * ClientConnectionMixin: receive (bundle-aware, cancellable), send/sent and
 * connect events. Proxy/exception branches dropped (those systems are not
 * ported in compat).
 */
@Mixin(ClientConnection.class)
public abstract class CompatPacketMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void compat$onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof BundleS2CPacket bundle) {
            for (Iterator<Packet<? super ClientPlayPacketListener>> it = bundle.getPackets().iterator(); it.hasNext(); ) {
                if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(it.next(), (ClientConnection) (Object) this)).isCancelled()) it.remove();
            }
        } else if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(packet, (ClientConnection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/network/NetworkingBackend;Lnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"))
    private static void compat$onConnect(InetSocketAddress address, NetworkingBackend backend, ClientConnection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        MeteorClient.EVENT_BUS.post(ServerConnectEndEvent.get(address));
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void compat$onSendPacketHead(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(new PacketEvent.Send(packet, (ClientConnection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("TAIL"))
    private void compat$onSendPacketTail(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new PacketEvent.Sent(packet, (ClientConnection) (Object) this));
    }
}
