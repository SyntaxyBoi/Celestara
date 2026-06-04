package moth.celestara.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import moth.celestara.event.CelestaraEvents;
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

import java.util.List;

public final class CelestaraSkyRenderer {
    private CelestaraSkyRenderer() {
    }

    public static void render(MatrixStack matrices, ClientWorld world, float tickDelta, boolean thickFog) {
        if (matrices == null || world == null || thickFog || !NightSkyPlanManager.isAllowedDimension(world)) {
            return;
        }

        float visibility = nightVisibility(world, tickDelta);
        boolean solarEclipse = CelestaraClientEventState.isSolarEclipseActive(world);
        boolean lunarEclipse = CelestaraClientEventState.isLunarEclipseActive(world);
        List<PersistentCometEvent> persistentComets = PersistentCometManager.activeComets(world, tickDelta);
        if (visibility <= CelestaraVisualConstants.MIN_VISIBILITY
                && !solarEclipse
                && !lunarEclipse
                && persistentComets.isEmpty()) {
            return;
        }

        float skyVisibility = solarEclipse
                ? Math.max(visibility, 0.44F * CelestaraClientEventState.solarEclipseFade(world))
                : visibility;
        long eclipsePlanIndex = eclipsePlanIndex(world, solarEclipse, lunarEclipse);
        NightSkyPlan plan = null;
        if (skyVisibility > CelestaraVisualConstants.MIN_VISIBILITY) {
            plan = eclipsePlanIndex == Long.MIN_VALUE
                    ? NightSkyPlanManager.getPlan(world)
                    : NightSkyPlanManager.getPlan(world, eclipsePlanIndex);
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
        double renderTime = world.getTime() + tickDelta;
        double nightOffset = NightSkyPlanManager.currentNightOffset(world, tickDelta);

        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        if (plan != null) {
            CelestaraStarRenderer.render(matrix, buffer, plan, skyVisibility, renderTime, skyRotationDegrees);
            CelestaraEffectRenderer.render(matrix, buffer, plan, nightOffset, renderTime, skyVisibility, 0.0F);
        }
        CelestaraEffectRenderer.renderPersistentComets(
                matrix,
                buffer,
                persistentComets,
                world.getTimeOfDay() + tickDelta,
                persistentVisibility(visibility, solarEclipse, lunarEclipse, world)
        );
        BufferRenderer.drawWithGlobalProgram(buffer.end());

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

    private static long eclipsePlanIndex(ClientWorld world, boolean solarEclipse, boolean lunarEclipse) {
        if (solarEclipse) {
            return CelestaraEvents.visibleSolarEclipseDay(world.getTimeOfDay());
        }
        if (lunarEclipse) {
            return CelestaraEvents.visibleLunarEclipseNight(world.getTimeOfDay());
        }
        return Long.MIN_VALUE;
    }

    private static float persistentVisibility(float nightVisibility, boolean solarEclipse,
                                              boolean lunarEclipse, ClientWorld world) {
        float solarFade = solarEclipse ? CelestaraClientEventState.solarEclipseFade(world) : 0.0F;
        float lunarFade = lunarEclipse ? CelestaraClientEventState.lunarEclipseFade(world) : 0.0F;
        float dayVisibility = solarEclipse ? 0.56F * solarFade : 0.18F;
        float lunarBoost = lunarEclipse ? 0.18F * lunarFade : 0.0F;
        return MathHelper.clamp(Math.max(nightVisibility, dayVisibility) + lunarBoost, 0.0F, 1.0F);
    }
}
