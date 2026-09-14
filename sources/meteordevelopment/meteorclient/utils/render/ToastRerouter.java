package meteordevelopment.meteorclient.utils.render;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.NotificationEvent;

/**
 * Reroutes Meteor toasts into LiquidBounce notifications.
 *
 * Every Meteor toast in the wild (ours, StashFinder-style modules, third
 * party addons) flows through {@code ToastManager.add}, which
 * CompatToastMixin intercepts. This is the LB side of that interception:
 * plain {@link String}s in, LB {@link NotificationEvent} out.
 *
 * Deliberately free of any {@code net.minecraft} types: LB's event bus
 * and notification classes are LB's own, so no remapping is ever needed
 * and this file compiles unchanged on every MC version.
 *
 * Fail-open contract: returns true only when LB actually accepted the
 * toast. On ANY error (LB absent, bus busy, ...) returns false and the
 * caller lets the vanilla toast through instead. Never crash the client
 * over a notification.
 */
public final class ToastRerouter {
    private ToastRerouter() {
    }

    public static boolean reroute(String title, String text) {
        try {
            String t = title != null ? title : "";
            String m = text != null ? text : "";
            EventManager.INSTANCE.callEvent(new NotificationEvent(t, m, NotificationEvent.Severity.INFO));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
