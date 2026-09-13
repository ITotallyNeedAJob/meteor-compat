package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * COMPAT 26.2: chunk data event source. Verbatim upstream port
 * (ClientPacketListenerMixin.handleLevelChunkWithLight): fires when a chunk
 * payload arrives - the backbone of ore/stash/chunk-finder addons.
 */
@Mixin(ClientPacketListener.class)
public abstract class CompatChunkMixin {
    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void compat$onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        LevelChunk chunk = mc.level.getChunk(packet.getX(), packet.getZ());
        MeteorClient.EVENT_BUS.post(new ChunkDataEvent(chunk));
    }
}
