package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import net.minecraft.client.gl.ShaderLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Phase 1: pipeline (re)compile hook. Verbatim upstream port: after
 * vanilla shaders (re)load, precompile our pipelines with the live resource
 * manager (which includes mod packs). Without this the GPU device compiles
 * them lazily on first draw with a provider that cannot see our
 * meteor-client:shaders/* sources, and every custom draw silently fails.
 */
@Mixin(ShaderLoader.class)
public abstract class CompatShaderLoaderMixin {
    @Inject(method = "apply(Lnet/minecraft/client/gl/ShaderLoader$Definitions;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("TAIL"))
    private void compat$reloadPipelines(CallbackInfo ci) {
        MeteorRenderPipelines.precompile();
    }
}
