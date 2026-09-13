package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT 26.2: 3D event source. Trimmed port of upstream GameRendererMixin
 * (onRenderLevel): no Freecam/NoRender/Zoom coupling, no Iris branch (compat
 * reports no shader pack in use).
 */
@Mixin(GameRenderer.class)
public abstract class CompatRenderMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    protected abstract void bobView(final CameraRenderState cameraState, final PoseStack poseStack);

    @Shadow
    protected abstract void bobHurt(final CameraRenderState cameraState, final PoseStack poseStack);

    @Unique
    private Renderer3D compat$renderer;

    @Unique
    private Renderer3D compat$depthRenderer;

    @Unique
    private final PoseStack compat$matrices = new PoseStack();

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
    private void compat$onRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "projectionMatrix") Matrix4f projectionMatrix, @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix, @Local(name = "worldPartialTicks") float worldPartialTicks, @Local(name = "bobStack") PoseStack bobStack) {
        if (!Utils.canUpdate()) return;

        Profiler.get().push(MeteorClient.MOD_ID + "_render");

        if (compat$renderer == null)
            compat$renderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES, MeteorRenderPipelines.WORLD_COLORED);
        if (compat$depthRenderer == null)
            compat$depthRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(bobStack, compat$renderer, compat$depthRenderer, worldPartialTicks, mainCamera.position().x, mainCamera.position().y, mainCamera.position().z);

        // Update model view matrix

        RenderSystem.getModelViewStack().pushMatrix().mul(modelViewMatrix);

        compat$matrices.pushPose();
        bobHurt(this.gameRenderState.levelRenderState.cameraRenderState, compat$matrices);
        if (minecraft.options.bobView().get()) {
            bobView(this.gameRenderState.levelRenderState.cameraRenderState, compat$matrices);
        }

        Matrix4f inverseBob = new Matrix4f(compat$matrices.last().pose()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        compat$matrices.popPose();

        // Call utility classes (no Iris branch: compat reports no shader pack in use)

        RenderUtils.updateScreenCenter(projectionMatrix, modelViewMatrix);
        NametagUtils.onRender(modelViewMatrix);

        // Render

        // COMPAT perf: skip the event alloc + dispatch when nothing listens.
        if (MeteorClient.EVENT_BUS.isListening(Render3DEvent.class)) {
            compat$renderer.begin();
            compat$depthRenderer.begin();
            MeteorClient.EVENT_BUS.post(event);
            compat$renderer.render(bobStack);
            compat$depthRenderer.render(bobStack);
        }

        // Revert model view matrix

        RenderSystem.getModelViewStack().popMatrix();

        Profiler.get().pop();
    }
}
