/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

public class BaritoneUtils {
    // COMPAT: always false (baritone pathing not compiled in - see PathManagers).
    public static boolean IS_AVAILABLE = false;

    private BaritoneUtils() {
    }

    public static String getPrefix() {
        return "";
    }
}
