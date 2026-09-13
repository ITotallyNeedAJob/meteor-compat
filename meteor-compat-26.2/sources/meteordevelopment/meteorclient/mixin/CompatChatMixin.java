package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT 26.2: chat command dispatch backstop (all chat sources route through
 * ClientPacketListener.sendChat). Uses Commands.tryRun so meteor commands
 * coexist with LiquidBounce's "." commands: handled input is consumed, the
 * rest passes through to LB/vanilla.
 */
@Mixin(ClientPacketListener.class)
public abstract class CompatChatMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void compat$onSendChat(String message, CallbackInfo ci, @Local(argsOnly = true, name = "content") LocalRef<String> messageRef) {
        if (Commands.tryRun(message)) {
            ci.cancel();
            return;
        }

        // Not a meteor command: keep the SendMessageEvent addon hook, but only
        // for plain chat (prefix input already fell through to LB above).
        String prefix;
        try {
            prefix = Config.get().prefix.get();
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
