/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import net.minecraft.item.Items;

public class Categories {
    public static final Category Combat = new Category("Combat", Items.GOLDEN_SWORD.getDefaultStack());
    public static final Category Player = new Category("Player", Items.ARMOR_STAND.getDefaultStack());
    public static final Category Movement = new Category("Movement", Items.DIAMOND_BOOTS.getDefaultStack());
    public static final Category Render = new Category("Render", Items.GLASS.getDefaultStack());
    public static final Category World = new Category("World", Items.GRASS_BLOCK.getDefaultStack());
    public static final Category Misc = new Category("Misc", Items.LAVA_BUCKET.getDefaultStack());

    public static boolean REGISTERING;

    public static void init() {
        REGISTERING = true;

        // Meteor
        Modules.registerCategory(Combat);
        Modules.registerCategory(Player);
        Modules.registerCategory(Movement);
        Modules.registerCategory(Render);
        Modules.registerCategory(World);
        Modules.registerCategory(Misc);

        // Addons (COMPAT: per-addon isolation. An obfuscated addon's
        // invokedynamic/reflection init may throw for a single missing member;
        // that must skip the addon, not kill the whole client boot, and must
        // still reset REGISTERING below.)
        for (MeteorAddon addon : AddonManager.ADDONS) {
            try {
                addon.onRegisterCategories();
            } catch (Throwable t) {
                MeteorClient.LOG.error("meteor-compat: exception during category registration for addon '{}'.", addon.name, t);
            }
        }

        REGISTERING = false;
    }
}
