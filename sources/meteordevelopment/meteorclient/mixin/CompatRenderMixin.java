package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Phase 1: 3D event source. Trimmed port of upstream GameRendererMixin
 * (onRenderWorld): no Freecam/NoRender/WidgetScreen coupling, no NametagUtils,
 * no Iris branch (compat reports no shader pack in use).
 */
@Mixin(GameRenderer.class)
public abstract class CompatRenderMixin {
    @Shadow
    @Final
    private Camera camera;

    @Shadow
    private void tiltViewWhenHurt(MatrixStack matrices, float tickProgress) {}

    @Shadow
    private void bobView(MatrixStack matrices, float tickProgress) {}

    @Unique
    private Renderer3D compat$renderer;

    @Unique
    private Renderer3D compat$depthRenderer;

    @Unique
    private final MatrixStack compat$matrices = new MatrixStack();

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void compat$onRenderWorld(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 1) Matrix4f position, @Local(ordinal = 0) float tickDelta, @Local MatrixStack matrixStack) {
        if (!Utils.canUpdate()) return;

        // Screen-center projection + nametag matrices stay fresh even when no
        // 3D module listens (HUD/UI triangles use them too).
        RenderUtils.updateScreenCenter(projection, position);
        NametagUtils.onRender(position);

        // COMPAT perf: skip the whole 3D block when nothing listens (no active
        // modules/addons consuming Render3D). Zero-cost idle path.
        if (!MeteorClient.EVENT_BUS.isListening(Render3DEvent.class)) return;

        Profilers.get().push(MeteorClient.MOD_ID + "_render");

        if (compat$renderer == null) compat$renderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES, MeteorRenderPipelines.WORLD_COLORED);
        if (compat$depthRenderer == null) compat$depthRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(matrixStack, compat$renderer, compat$depthRenderer, tickDelta, camera.getCameraPos().x, camera.getCameraPos().y, camera.getCameraPos().z);

        RenderSystem.getModelViewStack().pushMatrix().mul(position);

        compat$matrices.push();
        tiltViewWhenHurt(compat$matrices, camera.getLastTickProgress());
        if (MeteorClient.mc.options.getBobView().getValue()) bobView(compat$matrices, camera.getLastTickProgress());

        Matrix4f inverseBob = new Matrix4f(compat$matrices.peek().getPositionMatrix()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        compat$matrices.pop();

        compat$renderer.begin();
        compat$depthRenderer.begin();
        MeteorClient.EVENT_BUS.post(event);
        compat$renderer.render(matrixStack);
        compat$depthRenderer.render(matrixStack);

        RenderSystem.getModelViewStack().popMatrix();

        Profilers.get().pop();
    }
}
