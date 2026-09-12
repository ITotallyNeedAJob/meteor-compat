package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * COMPAT Stage 3: text renderer facade. Verbatim upstream port except get():
 * custom bitmap fonts (Fonts/CustomTextRenderer/FontFace stack) are not
 * ported yet, so the vanilla renderer is always used. The full interface is
 * kept so addons calling beginBig/render/getWidth compile and run.
 */
public interface TextRenderer {
    static TextRenderer get() {
        return VanillaTextRenderer.INSTANCE;
    }

    void setAlpha(double a);

    void begin(double scale, boolean scaleOnly, boolean big);

    default void begin(double scale) { begin(scale, false, false); }

    default void begin() { begin(1, false, false); }

    default void beginBig() { begin(1, false, true); }

    double getWidth(String text, int length, boolean shadow);

    default double getWidth(String text, boolean shadow) { return getWidth(text, text.length(), shadow); }

    default double getWidth(String text) { return getWidth(text, text.length(), false); }

    double getHeight(boolean shadow);

    default double getHeight() { return getHeight(false); }

    double render(String text, double x, double y, Color color, boolean shadow);

    default double render(String text, double x, double y, Color color) { return render(text, x, y, color, false); }

    boolean isBuilding();

    void end();
}
