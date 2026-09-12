package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * COMPAT Stage 3: interaction-manager bridge. Minimal port of upstream
 * ClientPlayerInteractionManagerMixin: only meteor$syncSelected (needed by
 * InvUtils). The packet-cancelling injections (break/attack/interact events)
 * land with the interaction event sources.
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class CompatInteractionMixin implements IClientPlayerInteractionManager {
    @Shadow
    protected abstract void syncSelectedSlot();

    @Override
    public void meteor$syncSelected() {
        syncSelectedSlot();
    }
}
