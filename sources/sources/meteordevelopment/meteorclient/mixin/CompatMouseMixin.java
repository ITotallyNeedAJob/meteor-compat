package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.platform.Window;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.blaze3d.platform.InputConstants.RELEASE;

/**
 * COMPAT 26.2: mouse event source. Verbatim upstream port of
 * MouseHandlerMixin.
 */
@Mixin(MouseHandler.class)
public abstract class CompatMouseMixin {
    @Shadow
    public abstract double getScaledXPos(Window window);

    @Shadow
    public abstract double getScaledYPos(Window window);

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void compat$onMouseButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        Input.setButtonState(rawButtonInfo.button(), action != RELEASE);

        MouseButtonEvent click = new MouseButtonEvent(getScaledXPos(minecraft.getWindow()), getScaledYPos(minecraft.getWindow()), rawButtonInfo);
        if (MeteorClient.EVENT_BUS.post(MouseClickEvent.get(click, KeyAction.get(action))).isCancelled()) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void compat$onMouseScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(MouseScrollEvent.get(yoffset)).isCancelled()) ci.cancel();
    }
}
