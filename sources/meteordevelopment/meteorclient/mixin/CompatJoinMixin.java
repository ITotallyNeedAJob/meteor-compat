package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EnterReconfigurationS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Phase 1+2: join/leave event sources. Mirrors upstream
 * ClientPlayNetworkHandlerMixin: GameLeftEvent fires on re-join (saves
 * outgoing world state) and on reconfiguration, GameJoinedEvent on join.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class CompatJoinMixin {
    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void compat$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        // Leaving the previous world (if any): persist systems first.
        MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        MeteorClient.EVENT_BUS.post(GameJoinedEvent.get());
    }

    @Inject(method = "onEnterReconfiguration", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER))
    private void compat$onEnterReconfiguration(EnterReconfigurationS2CPacket packet, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
    }
}
