package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IChatHud;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * COMPAT Phase 1: minimal IChatHud implementor. Upstream ChatHudMixin carries
 * BetterChat coupling (longer messages, timestamps, ids); compat only needs
 * add() for command/feedback output, so message ids are ignored.
 */
@Mixin(ChatHud.class)
public abstract class CompatChatHudMixin implements IChatHud {
    @Shadow
    public abstract void addMessage(Text message);

    @Override
    public void meteor$add(Text message, int id) {
        addMessage(message);
    }
}
