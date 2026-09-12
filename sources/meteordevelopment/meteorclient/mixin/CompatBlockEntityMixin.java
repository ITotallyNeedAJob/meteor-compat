package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Stage 3: block-entity render event source. Verbatim upstream port
 * of BlockEntityRenderManagerMixin: cancellable, fires per block entity —
 * the backbone of storage-ESP addons.
 */
@Mixin(BlockEntityRenderManager.class)
public abstract class CompatBlockEntityMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntityRenderState> void compat$onRender(S renderState, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(RenderBlockEntityEvent.get(renderState)).isCancelled()) ci.cancel();
    }
}
