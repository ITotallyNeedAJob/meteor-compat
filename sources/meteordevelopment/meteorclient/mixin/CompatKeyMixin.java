package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT Stage 3: keyboard event source. Trimmed upstream port of
 * KeyboardMixin: no WidgetScreen/GUI branches (no ClickGUI in compat) and no
 * CharTypedEvent (only GUI text fields consume it). Uses build.3 record
 * component accessors (comp_4795/4796/4797).
 */
@Mixin(Keyboard.class)
public abstract class CompatKeyMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void compat$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        int key = input.comp_4795();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;

        int modifiers = input.comp_4797();
        // on Linux/X11 the modifier is not active when the key is pressed and still active when the key is released
        // https://github.com/glfw/glfw/issues/1630
        if (action == GLFW.GLFW_PRESS) {
            modifiers |= Input.getModifier(key);
        } else if (action == GLFW.GLFW_RELEASE) {
            modifiers &= ~Input.getModifier(key);
        }

        Input.setKeyState(key, action != GLFW.GLFW_RELEASE);
        boolean cancelled = MeteorClient.EVENT_BUS.post(KeyEvent.get(new KeyInput(key, input.comp_4796(), modifiers), KeyAction.get(action))).isCancelled();
        if (cancelled) ci.cancel();
    }
}
