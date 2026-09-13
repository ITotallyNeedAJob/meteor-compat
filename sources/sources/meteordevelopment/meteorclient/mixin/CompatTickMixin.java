package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT 26.2: tick event source. Upstream equivalent lives in
 * MinecraftMixin (tick HEAD/TAIL); kept minimal here on purpose.
 */
@Mixin(Minecraft.class)
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
