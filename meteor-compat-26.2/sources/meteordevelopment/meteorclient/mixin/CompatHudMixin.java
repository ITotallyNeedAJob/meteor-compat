package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.MeteorMcGuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * COMPAT 26.2: 2D event source with drawing support. Verbatim upstream port
 * of GuiRendererMixin: posts Render2DEvent into a dedicated Meteor render
 * state so addon/HUD drawing actually reaches the screen.
 */
@Mixin(GuiRenderer.class)
public abstract class CompatHudMixin {
    @Unique
    private GuiRenderState compat$renderState;

    @Unique
    private MeteorMcGuiRenderer compat$guiRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void compat$init(GuiRenderState renderState, FeatureRenderDispatcher featureRenderDispatcher, List<PictureInPictureRenderer<?>> pictureInPictureRenderers, CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof MeteorMcGuiRenderer) return;

        this.compat$renderState = new GuiRenderState();

        compat$guiRenderer = new MeteorMcGuiRenderer(
            this.compat$renderState,
            featureRenderDispatcher,
            pictureInPictureRenderers
        );
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void compat$preGui(CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof MeteorMcGuiRenderer) return;
        var mc = Minecraft.getInstance();

        if (mc.gui.screen() == null || mc.gui.screen() instanceof WidgetScreen) return;
        compat$render2D(mc);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void compat$postGui(CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof MeteorMcGuiRenderer) return;
        var mc = Minecraft.getInstance();

        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.gameRenderer.mainRenderTarget().getDepthTexture(), 1.0);

        if (mc.gui.screen() == null || mc.gui.screen() instanceof WidgetScreen) {
            compat$render2D(mc);
        }

        compat$guiRenderer.endFrame();
    }

    @Unique
    private void compat$render2D(Minecraft mc) {
        // COMPAT perf (Electron idea, native): skip the event alloc + full
        // listener dispatch when nothing consumes 2D (HUD off, no 2D addons).
        if (!MeteorClient.EVENT_BUS.isListening(Render2DEvent.class)) return;

        var mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        var mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (Utils.canUpdate() || HudEditorScreen.isOpen()) {
            Profiler.get().push(MeteorClient.MOD_ID + "_render_2d");
            Utils.unscaledProjection();

            var graphics = new GuiGraphicsExtractor(mc, compat$renderState, mouseX, mouseY);
            var tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

            MeteorClient.EVENT_BUS.post(Render2DEvent.get(graphics, graphics.guiWidth(), graphics.guiHeight(), tickDelta));
            compat$guiRenderer.render();

            Utils.scaledProjection();
            Profiler.get().pop();
        }

        if (mc.gui.screen() instanceof WidgetScreen widgetScreen) {
            var graphics = new GuiGraphicsExtractor(mc, compat$renderState, mouseX, mouseY);
            var guiDelta = mc.getDeltaTracker().getGameTimeDeltaTicks();

            widgetScreen.renderCustom(graphics, mouseX, mouseY, guiDelta);
            compat$guiRenderer.render();
        }
    }
}
