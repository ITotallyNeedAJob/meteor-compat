package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Stage 3: chunk data event source. Verbatim upstream port
 * (ClientPlayNetworkHandlerMixin.onChunkData): fires when a chunk payload
 * arrives — the backbone of ore/stash/chunk-finder addons.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class CompatChunkMixin {
    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void compat$onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        WorldChunk chunk = MeteorClient.mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());
        MeteorClient.EVENT_BUS.post(new ChunkDataEvent(chunk));
    }
}
