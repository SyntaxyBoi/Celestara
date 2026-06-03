package moth.celestara.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class CelestaraSkyRenderer {
    private CelestaraSkyRenderer() {
    }

    public static void render(MatrixStack matrices, ClientWorld world, float tickDelta, boolean thickFog) {
        if (matrices == null || world == null || thickFog || !NightSkyPlanManager.isAllowedDimension(world)) {
            return;
        }

        float visibility = nightVisibility(world, tickDelta);
        if (visibility <= CelestaraVisualConstants.MIN_VISIBILITY) {
            return;
        }

        NightSkyPlan plan = NightSkyPlanManager.getPlan(world);
        if (plan == null) {
            return;
        }

        matrices.push();
        float skyRotationDegrees = world.getSkyAngle(tickDelta) * 360.0F;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        double renderTime = world.getTime() + tickDelta;
        double nightOffset = NightSkyPlanManager.currentNightOffset(world, tickDelta);
        CelestaraStarRenderer.render(matrix, buffer, plan, visibility, renderTime, skyRotationDegrees);
        CelestaraEffectRenderer.render(matrix, buffer, plan, nightOffset, renderTime, visibility, 0.0F);
        BufferBuilder.BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static float nightVisibility(ClientWorld world, float tickDelta) {
        // Yarn 1.20.1 leaves this ClientWorld method intermediary-named; it is vanilla star brightness.
        float starBrightness = world.method_23787(tickDelta);
        float rainFade = 1.0F - world.getRainGradient(tickDelta) * 0.62F;
        float thunderFade = 1.0F - world.getThunderGradient(tickDelta) * 0.24F;
        return MathHelper.clamp(starBrightness * rainFade * thunderFade, 0.0F, 1.0F);
    }
}
