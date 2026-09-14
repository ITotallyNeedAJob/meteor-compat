/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.ToastRerouter;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COMPAT: reroutes Meteor toasts into LiquidBounce notifications.
 *
 * Intercepts every toast handed to the vanilla manager. Only
 * {@link MeteorToast} is rerouted (vanilla advancement/recipe/system
 * toasts keep rendering natively); on success the vanilla add is
 * cancelled so the toast shows exactly once, LB-styled. Fail-open:
 * if LB rejects it for any reason the vanilla toast goes through.
 */
@Mixin(ToastManager.class)
public abstract class CompatToastMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void compat$rerouteMeteorToast(Toast toast, CallbackInfo ci) {
        if (!(toast instanceof MeteorToast)) return;

        MeteorToastAccessor acc = (MeteorToastAccessor) toast;
        String title = acc.getTitle().getString();
        Text textObj = acc.getText();
        String text = textObj != null ? textObj.getString() : "";

        if (ToastRerouter.reroute(title, text)) ci.cancel();
    }
}
