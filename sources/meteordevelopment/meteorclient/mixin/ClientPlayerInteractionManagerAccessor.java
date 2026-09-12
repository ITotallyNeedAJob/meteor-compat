package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * COMPAT: verbatim upstream accessor (field names verified against yarn
 * build.3 via javap: currentBreakingProgress / currentBreakingPos).
 */
@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("currentBreakingProgress")
    float meteor$getBreakingProgress();

    @Accessor("currentBreakingProgress")
    void meteor$setCurrentBreakingProgress(float progress);

    @Accessor("currentBreakingPos")
    BlockPos meteor$getCurrentBreakingBlockPos();
}
