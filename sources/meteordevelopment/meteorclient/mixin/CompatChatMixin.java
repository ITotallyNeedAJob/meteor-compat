package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Phase 1: chat command dispatch backstop (non-chat-screen sources).
 * Uses Commands.tryRun so meteor commands coexist with LiquidBounce's "."
 * commands: handled input is consumed, the rest passes through to LB/vanilla.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class CompatChatMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void compat$onSendChatMessage(String message, CallbackInfo ci, @Local(argsOnly = true) LocalRef<String> messageRef) {
        if (Commands.tryRun(message)) {
            ci.cancel();
            return;
        }

        // Not a meteor command: keep the SendMessageEvent addon hook, but only
        // for plain chat (prefix input already fell through to LB above).
        String prefix;
        try {
            prefix = meteordevelopment.meteorclient.systems.config.Config.get().prefix.get();
        } catch (Throwable t) {
            prefix = ".";
        }

        if (!message.startsWith(prefix)) {
            SendMessageEvent event = MeteorClient.EVENT_BUS.post(SendMessageEvent.get(message));

            if (!event.isCancelled()) {
                messageRef.set(event.message);
            } else {
                ci.cancel();
            }
        }
    }
}
