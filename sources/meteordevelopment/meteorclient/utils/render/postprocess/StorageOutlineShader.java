package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;

public class StorageOutlineShader extends PostProcessShader {
    // COMPAT: StorageESP removed - shader never draws.
    public StorageOutlineShader() {
        super(MeteorRenderPipelines.POST_OUTLINE);
    }

    @Override
    protected boolean shouldDraw() {
        return false;
    }

    @Override
    protected void setupPass(MeshRenderer renderer) {
    }
}
