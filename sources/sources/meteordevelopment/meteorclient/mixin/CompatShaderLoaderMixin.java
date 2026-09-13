package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT 26.2: pipeline (re)compile hook. Verbatim upstream port
 * (ShaderManagerMixin): after vanilla shaders (re)load, precompile our
 * pipelines with the live resource manager (which includes mod packs).
 */
@Mixin(ShaderManager.class)
public abstract class CompatShaderLoaderMixin {
    @Inject(method = "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void compat$reloadPipelines(CallbackInfo ci) {
        MeteorRenderPipelines.precompile();
    }
}
