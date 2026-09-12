package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PostProcessShaders {
    public static EntityShader CHAMS;
    public static EntityShader ENTITY_OUTLINE;
    public static PostProcessShader STORAGE_OUTLINE;

    private PostProcessShaders() {}

    private static boolean initialized;

    @PreInit
    public static void init() {
        // COMPAT: shader constructors allocate GPU textures, which do not exist
        // at client entrypoint. Lazily initialized on first render instead.
        if (initialized) return;
        initialized = true;

        CHAMS = new ChamsShader();
        ENTITY_OUTLINE = new EntityOutlineShader();
        STORAGE_OUTLINE = new StorageOutlineShader();

        MeteorClient.EVENT_BUS.subscribe(PostProcessShaders.class);
    }

    private static void ensureInit() {
        if (!initialized && mc != null && mc.getWindow() != null) {
            try {
                init();
            }
            catch (Throwable t) {
                initialized = false;
                MeteorClient.LOG.debug("PostProcessShaders not ready yet.");
            }
        }
    }

    public static void beginRender() {
        ensureInit();
        if (!initialized) return;
        CHAMS.clearTexture();
        ENTITY_OUTLINE.clearTexture();
        STORAGE_OUTLINE.clearTexture();
    }

    public static void submitEntityVertices() {
        if (!initialized) return;
        CHAMS.submitVertices();
        ENTITY_OUTLINE.submitVertices();
    }

    @EventHandler
    private static void onRender(Render2DEvent event) {
        CHAMS.render();
        ENTITY_OUTLINE.render();
    }

    public static void onResized(int width, int height) {
        if (mc == null) return;

        CHAMS.onResized(width, height);
        ENTITY_OUTLINE.onResized(width, height);
        STORAGE_OUTLINE.onResized(width, height);
    }
}
