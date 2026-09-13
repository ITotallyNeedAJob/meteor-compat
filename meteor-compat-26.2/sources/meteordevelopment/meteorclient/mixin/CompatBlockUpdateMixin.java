package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * COMPAT 26.2: block update event source. Verbatim upstream port of
 * LevelChunkMixin: fires for client-side block changes.
 */
@Mixin(LevelChunk.class)
public abstract class CompatBlockUpdateMixin {
    @Shadow
    @Final
    private Level level;

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void compat$onSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (level.isClientSide()) MeteorClient.EVENT_BUS.post(BlockUpdateEvent.get(pos, cir.getReturnValue(), state));
    }
}
