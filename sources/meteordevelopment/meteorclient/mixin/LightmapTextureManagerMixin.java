/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    @Final
    private GpuTexture glTexture;

    // COMPAT perf (Electron idea, native): the white clear only needs to run
    // once per Fullbright-gamma stretch; later frames skip the GPU work.
    @Unique
    private boolean compat$lightmapCleared;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", shift = At.Shift.AFTER), cancellable = true)
    private void update$skip(float tickProgress, CallbackInfo ci, @Local Profiler profiler) {
        if (Modules.get().get(Fullbright.class).getGamma()) { // COMPAT: Xray branch removed (module not bundled)
            profiler.pop();
            if (!compat$lightmapCleared) {
                compat$lightmapCleared = true;
                RenderSystem.getDevice().createCommandEncoder().clearColorTexture(glTexture, ColorHelper.getArgb(255, 255, 255, 255));
            }
            ci.cancel();
        }
        else if (compat$lightmapCleared) {
            compat$lightmapCleared = false;
        }
    }

    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
	private void getDarknessFactor(LivingEntity entity, float factor, float tickProgress, CallbackInfoReturnable<Float> info) {
		if (Modules.get().get(NoRender.class).noDarkness()) info.setReturnValue(0.0f);
	}
}
