package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IMultiPlayerGameMode;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * COMPAT 26.2: interaction-manager bridge. Minimal port of upstream
 * MultiPlayerGameModeMixin: only meteor$syncSelected (needed by InvUtils).
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class CompatInteractionMixin implements IMultiPlayerGameMode {
    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    @Override
    public void meteor$syncSelected() {
        ensureHasSentCarriedItem();
    }
}
