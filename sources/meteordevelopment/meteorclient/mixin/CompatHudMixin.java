package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Phase 1: 2D event source. Upstream equivalent lives in
 * InGameHudMixin (onRender). Note: upstream passes scaled width twice
 * (width, width); compat passes width, height correctly.
 */
@Mixin(InGameHud.class)
public abstract class CompatHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void compat$onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // COMPAT perf (Electron idea, native): skip the event alloc + full
        // listener dispatch when nothing consumes 2D (HUD off, no 2D addons).
        if (!MeteorClient.EVENT_BUS.isListening(Render2DEvent.class)) return;
        MeteorClient.EVENT_BUS.post(Render2DEvent.get(context, context.getScaledWindowWidth(), context.getScaledWindowHeight(), tickCounter.getTickProgress(true)));
    }
}
