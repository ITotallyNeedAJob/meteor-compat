package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import net.minecraft.world.entity.Entity;

public class EntityOutlineShader extends EntityShader {
    // COMPAT: ESP removed - shader never draws.

    public EntityOutlineShader() {
        super(MeteorRenderPipelines.POST_OUTLINE);
    }

    @Override
    protected boolean shouldDraw() {
        return false;
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        return false;
    }

    @Override
    protected void setupPass(MeshRenderer renderer) {
    }
}
