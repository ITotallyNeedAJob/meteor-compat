package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT 26.2: join/leave event sources. Mirrors upstream
 * ClientPacketListenerMixin: GameLeftEvent fires on re-login (saves outgoing
 * world state) and on reconfiguration, GameJoinedEvent on login.
 */
@Mixin(ClientPacketListener.class)
public abstract class CompatJoinMixin {
    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void compat$onLoginHead(ClientboundLoginPacket packet, CallbackInfo ci) {
        // Leaving the previous world (if any): persist systems first.
        MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void compat$onLoginTail(ClientboundLoginPacket packet, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(GameJoinedEvent.get());
    }

    // the server sends a GameJoin packet after the reconfiguration phase
    @Inject(method = "handleConfigurationStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void compat$onConfigurationStart(ClientboundStartConfigurationPacket packet, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
    }
}
