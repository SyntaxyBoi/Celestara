package moth.celestara.client.sky;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class CelestaraStarRenderer {
    private CelestaraStarRenderer() {
    }

    public static boolean render(Matrix4f matrix, BufferBuilder buffer, NightSkyPlan plan, float visibility,
                                 double time, float skyRotationDegrees) {
        if (!CelestaraVisualConstants.ENHANCED_STARS_ENABLED || visibility <= CelestaraVisualConstants.MIN_VISIBILITY) {
            return false;
        }

        boolean emitted = false;
        for (StarVisualData star : plan.stars()) {
            float alphaScale = starAlphaScale(star, visibility, time);
            int alpha = Math.round(185.0F * alphaScale);
            if (alpha <= 3) {
                continue;
            }

            float layerRotation = skyRotationDegrees * star.layerSpeed();
            Vec3d direction = CelestaraSkyGeometry.rotateX(star.direction(), layerRotation);
            Vec3d center = CelestaraSkyGeometry.skyPosition(direction);
            float rotation = star.baseRotation()
                    + MathHelper.sin((float) (time * star.oscillationSpeed() + star.oscillationPhase())) * star.maxRotationOffset();
            double radians = Math.toRadians(rotation);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            Vec3d baseRight = CelestaraSkyGeometry.rotateX(star.basis().right(), layerRotation);
            Vec3d baseUp = CelestaraSkyGeometry.rotateX(star.basis().up(), layerRotation);
            Vec3d right = baseRight.multiply(cos).add(baseUp.multiply(sin));
            Vec3d up = baseUp.multiply(cos).subtract(baseRight.multiply(sin));

            int starColor = star.color();
            if (star.size() > 0.092F) {
                CelestaraSkyGeometry.emitDiamond(
                        matrix,
                        buffer,
                        center,
                        right,
                        up,
                        star.size() * 1.75F,
                        star.size() * 1.75F,
                        starColor,
                        Math.round(alpha * 0.18F)
                );
            }
            CelestaraSkyGeometry.emitDiamond(
                    matrix,
                    buffer,
                    center,
                    right,
                    up,
                    star.size(),
                    star.size() * 1.12F,
                    starColor,
                    alpha
            );
            if (star.size() > 0.118F) {
                CelestaraSkyGeometry.emitDiamond(
                        matrix,
                        buffer,
                        center,
                        up,
                        right,
                        star.size() * 0.52F,
                        star.size() * 1.42F,
                        starColor,
                        Math.round(alpha * 0.34F)
                );
            }
            emitted = true;
        }
        return emitted;
    }

    private static float starAlphaScale(StarVisualData star, float visibility, double time) {
        float primary = MathHelper.sin((float) (time * star.twinkleSpeed() + star.twinklePhase()));
        float secondary = MathHelper.sin((float) (time * star.twinkleSpeed() * 0.37D + star.twinklePhase() * 1.7D));
        float twinkle = 1.0F + primary * star.twinkleAmplitude() + secondary * star.twinkleAmplitude() * 0.32F;
        return MathHelper.clamp(star.baseBrightness() * twinkle * visibility, 0.0F, 1.0F);
    }
}
