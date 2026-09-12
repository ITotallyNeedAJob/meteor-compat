package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.commands.Commands;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT: chat-screen entry point so meteor "." commands run ALONGSIDE
 * LiquidBounce's own "." commands. Priority 500 runs before LB's ChatScreen
 * hook (priority 1337): meteor-owned input is consumed here, everything else
 * falls through to LB untouched. Addon code is not involved.
 *
 * NOTE: yarn build.3 names this ChatScreen.sendMessage (LB targets the mojmap
 * name handleChatMessage, which does not exist here) - verified via javap on
 * the yarn-merged 1.21.11 jar. All injection targets must use yarn names.
 */
@Mixin(value = ChatScreen.class, priority = 500)
public abstract class CompatChatScreenMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void compat$onChatMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (Commands.tryRun(message)) ci.cancel();
    }
}
