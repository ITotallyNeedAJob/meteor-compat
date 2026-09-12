package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Phase 1: tick event source. Upstream equivalent lives in
 * MinecraftClientMixin (onPreTick/onTick); kept minimal here on purpose.
 */
@Mixin(MinecraftClient.class)
public abstract class CompatTickMixin {
    @Inject(at = @At("HEAD"), method = "tick")
    private void compat$onPreTick(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(TickEvent.Pre.get());
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void compat$onPostTick(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(TickEvent.Post.get());
    }
}
