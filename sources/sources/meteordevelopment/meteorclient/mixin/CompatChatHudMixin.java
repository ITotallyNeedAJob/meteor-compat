package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IChatHud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * COMPAT 26.2: minimal IChatHud implementor (needed by ChatUtils). Upstream
 * ChatComponentMixin carries BetterChat coupling; compat only needs add() for
 * command/feedback output, so message ids are ignored.
 */
@Mixin(ChatComponent.class)
public abstract class CompatChatHudMixin implements IChatHud {
    @Shadow
    public abstract void addClientSystemMessage(Component message);

    @Override
    public void meteor$add(Component message, int id) {
        addClientSystemMessage(message);
    }
}
