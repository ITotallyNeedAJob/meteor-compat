package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT 26.2: keyboard event source. Trimmed port of upstream
 * KeyboardHandlerMixin: no WidgetScreen/GUI branches (compat drives module
 * keybinds globally, same tradeoff as the 1.21.11 host).
 */
@Mixin(KeyboardHandler.class)
public abstract class CompatKeyMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void compat$onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (event.key() == InputConstants.UNKNOWN.getValue()) return;

        int modifiers = event.modifiers();
        // on Linux/X11 the modifier is not active when the key is pressed and still active when the key is released
        // https://github.com/glfw/glfw/issues/1630
        if (action == InputConstants.PRESS) {
            modifiers |= Input.getModifier(event.key());
        } else if (action == InputConstants.RELEASE) {
            modifiers &= ~Input.getModifier(event.key());
        }

        Input.setKeyState(event.key(), action != InputConstants.RELEASE);
        if (MeteorClient.EVENT_BUS.post(KeyInputEvent.get(new KeyEvent(event.key(), event.scancode(), modifiers), KeyAction.get(action))).isCancelled()) {
            ci.cancel();
        }
    }
}
