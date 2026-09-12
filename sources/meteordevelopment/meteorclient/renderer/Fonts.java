/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.FontUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Fonts {
    public static final String[] BUILTIN_FONTS = { "JetBrains Mono", "Comfortaa", "Tw Cen MT", "Pixelation" };

    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;

    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    public static CustomTextRenderer RENDERER;

    private Fonts() {
    }

    private static boolean refreshed;
    private static int retryCooldown;

    // COMPAT: upstream runs refresh() via @PreInit at client entrypoint, but font
    // textures need the GPU. Retry on ticks until it succeeds (first tick runs
    // after game init, when the device exists). Throttled so a persistent
    // failure can never spin (each attempt reloads all system fonts).
    @meteordevelopment.orbit.EventHandler
    private static void onTick(TickEvent.Post event) {
        if (refreshed) return;
        if (mc == null || mc.getWindow() == null) return;
        if (retryCooldown-- > 0) return;
        retryCooldown = 40;

        try {
            refresh();
            refreshed = RENDERER != null;
        }
        catch (Throwable t) {
            MeteorClient.LOG.warn("Fonts refresh failed, will retry: {}", t.toString());
        }
    }

    @PreInit
    public static void refresh() {
        FONT_FAMILIES.clear();

        for (String builtinFont : BUILTIN_FONTS) {
            FontUtils.loadBuiltin(FONT_FAMILIES, builtinFont);
        }

        for (String fontPath : FontUtils.getSearchPaths()) {
            FontUtils.loadSystem(FONT_FAMILIES, new File(fontPath));
        }

        FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));

        MeteorClient.LOG.info("Found {} font families.", FONT_FAMILIES.size());

        DEFAULT_FONT_FAMILY = FontUtils.getBuiltinFontInfo(BUILTIN_FONTS[1]).family();
        // COMPAT: never NPE if a builtin font asset is missing; fall back to
        // whatever family is available so boot can continue.
        FontFamily defaultFamily = getFamily(DEFAULT_FONT_FAMILY);
        if (defaultFamily == null && !FONT_FAMILIES.isEmpty()) {
            defaultFamily = FONT_FAMILIES.get(0);
            DEFAULT_FONT_FAMILY = defaultFamily.getName();
            MeteorClient.LOG.warn("Default font family missing, using '{}' instead.", DEFAULT_FONT_FAMILY);
        }
        if (defaultFamily == null) throw new IllegalStateException("No font families loaded.");
        DEFAULT_FONT = defaultFamily.get(FontInfo.Type.Regular);

        Config config = Config.get();
        load(config != null ? config.font.get() : DEFAULT_FONT);
    }

    public static void load(FontFace fontFace) {
        // COMPAT: config load at client entrypoint fires onChanged before the GPU
        // exists (and before refresh() ever ran). Defer instead of crashing.
        if (fontFace == null) fontFace = DEFAULT_FONT;
        if (fontFace == null) return;

        if (RENDERER != null) {
            if (RENDERER.fontFace.equals(fontFace)) return;
            else RENDERER.destroy();
        }

        try {
            RENDERER = new CustomTextRenderer(fontFace);
            MeteorClient.EVENT_BUS.post(CustomFontChangedEvent.get());
        }
        catch (Exception e) {
            if (fontFace != null && fontFace.equals(DEFAULT_FONT)) {
                throw new RuntimeException("Failed to load default font: " + fontFace, e);
            }

            MeteorClient.LOG.error("Failed to load font: {}", fontFace, e);
            if (DEFAULT_FONT != null) load(Fonts.DEFAULT_FONT);
        }

        if (mc.currentScreen instanceof WidgetScreen && Config.get().customFont.get()) {
            ((WidgetScreen) mc.currentScreen).invalidate();
        }
    }

    public static FontFamily getFamily(String name) {
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.getName().equalsIgnoreCase(name)) {
                return fontFamily;
            }
        }

        return null;
    }
}
