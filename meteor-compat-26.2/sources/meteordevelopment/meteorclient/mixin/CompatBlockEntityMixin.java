package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT 26.2: block-entity render event source. Verbatim upstream port of
 * BlockEntityRenderDispatcherMixin: cancellable, fires per block entity -
 * the backbone of storage-ESP addons.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class CompatBlockEntityMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntityRenderState> void compat$onSubmit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        RenderBlockEntityEvent event = MeteorClient.EVENT_BUS.post(RenderBlockEntityEvent.get(state));
        if (event.isCancelled()) ci.cancel();
    }
}
