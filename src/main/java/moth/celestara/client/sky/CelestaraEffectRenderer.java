package moth.celestara.client.sky;

import moth.butterflyapi.math.Vecs;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public final class CelestaraEffectRenderer {
    private static final Vec3d RIGHT = new Vec3d(1.0D, 0.0D, 0.0D);
    private static final Vec3d UP = new Vec3d(0.0D, 1.0D, 0.0D);

    private CelestaraEffectRenderer() {
    }

    public static boolean render(Matrix4f matrix, BufferBuilder buffer, NightSkyPlan plan, double nightOffset,
                                 double time, float visibility, float skyRotationDegrees) {
        if (!CelestaraVisualConstants.MOVING_EFFECTS_ENABLED
                || nightOffset < 0.0D
                || visibility <= CelestaraVisualConstants.MIN_VISIBILITY) {
            return false;
        }

        boolean emitted = false;
        emitted |= renderShootingStars(matrix, buffer, plan, nightOffset, time, visibility, skyRotationDegrees);
        emitted |= renderComets(matrix, buffer, plan, nightOffset, time, visibility, skyRotationDegrees);
        return emitted;
    }

    public static boolean renderPersistentComets(Matrix4f matrix, BufferBuilder buffer,
                                                 List<PersistentCometEvent> comets, double worldTime,
                                                 float visibility) {
        if (!CelestaraVisualConstants.MOVING_EFFECTS_ENABLED
                || comets.isEmpty()
                || visibility <= CelestaraVisualConstants.MIN_VISIBILITY) {
            return false;
        }

        boolean emitted = false;
        for (PersistentCometEvent comet : comets) {
            if (comet.isActive(worldTime)) {
                renderPersistentComet(matrix, buffer, comet, worldTime, visibility);
                emitted = true;
            }
        }
        return emitted;
    }

    private static boolean renderShootingStars(Matrix4f matrix, BufferBuilder buffer, NightSkyPlan plan,
                                               double nightOffset, double time, float visibility, float skyRotationDegrees) {
        List<ShootingStarEvent> events = plan.shootingStars();
        int endExclusive = upperBoundShootingStars(events, nightOffset);
        boolean emitted = false;
        for (int index = endExclusive - 1; index >= 0; index--) {
            ShootingStarEvent event = events.get(index);
            if (event.startOffset() + plan.maxShootingStarDuration() + 4 < nightOffset) {
                break;
            }
            if (event.isActive(nightOffset)) {
                renderShootingStar(matrix, buffer, event, nightOffset, time, visibility, skyRotationDegrees);
                emitted = true;
            }
        }
        return emitted;
    }

    private static boolean renderComets(Matrix4f matrix, BufferBuilder buffer, NightSkyPlan plan,
                                        double nightOffset, double time, float visibility, float skyRotationDegrees) {
        boolean emitted = false;
        for (CometEvent comet : plan.comets()) {
            if (comet.startOffset() + plan.maxCometDuration() + 4 < nightOffset) {
                continue;
            }
            if (comet.startOffset() > nightOffset) {
                break;
            }
            if (comet.isActive(nightOffset)) {
                renderComet(matrix, buffer, comet, nightOffset, time, visibility, skyRotationDegrees);
                emitted = true;
            }
        }
        return emitted;
    }

    private static void renderShootingStar(Matrix4f matrix, BufferBuilder buffer, ShootingStarEvent event,
                                           double nightOffset, double time, float visibility, float skyRotationDegrees) {
        float progress = MathHelper.clamp(event.progress(nightOffset), 0.0F, 1.0F);
        float fade = smoothstep(0.0F, 0.08F, progress) * (1.0F - smoothstep(0.96F, 1.0F, progress));
        float alphaScale = visibility * event.brightness() * fade;
        if (alphaScale <= 0.01F) {
            return;
        }

        renderTrail(
                matrix,
                buffer,
                event.path(),
                progress,
                event.trailProgressLength(),
                event.size(),
                event.color(),
                Math.round(185.0F * alphaScale),
                CelestaraVisualConstants.TRAIL_SEGMENTS,
                false,
                skyRotationDegrees
        );
        renderHead(
                matrix,
                buffer,
                event.path(),
                progress,
                event.size(),
                event.headRotation() + (float) (time * event.spinSpeed()),
                event.color(),
                Math.round(218.0F * alphaScale),
                false,
                skyRotationDegrees
        );
    }

    private static void renderComet(Matrix4f matrix, BufferBuilder buffer, CometEvent comet,
                                    double nightOffset, double time, float visibility, float skyRotationDegrees) {
        float progress = MathHelper.clamp(comet.progress(nightOffset), 0.0F, 1.0F);
        float fade = smoothstep(0.0F, 0.08F, progress) * (1.0F - smoothstep(0.97F, 1.0F, progress));
        float alphaScale = visibility * comet.brightness() * fade;
        if (alphaScale <= 0.01F) {
            return;
        }

        renderTrail(
                matrix,
                buffer,
                comet.path(),
                progress,
                comet.trailProgressLength(),
                comet.size(),
                comet.color(),
                Math.round(220.0F * alphaScale),
                CelestaraVisualConstants.COMET_TRAIL_SEGMENTS,
                true,
                skyRotationDegrees
        );
        renderHead(
                matrix,
                buffer,
                comet.path(),
                progress,
                comet.size(),
                comet.headRotation() + (float) (time * comet.spinSpeed()),
                comet.color(),
                Math.round(230.0F * alphaScale),
                true,
                skyRotationDegrees
        );
    }

    private static void renderPersistentComet(Matrix4f matrix, BufferBuilder buffer, PersistentCometEvent comet,
                                              double worldTime, float visibility) {
        float progress = MathHelper.clamp(comet.progress(worldTime), 0.0F, 1.0F);
        float alphaScale = visibility * comet.brightness() * comet.fade(worldTime);
        if (alphaScale <= 0.01F) {
            return;
        }

        renderTrail(
                matrix,
                buffer,
                comet.path(),
                progress,
                comet.trailProgressLength(),
                comet.size(),
                comet.color(),
                Math.round(218.0F * alphaScale),
                CelestaraVisualConstants.PERSISTENT_COMET_TRAIL_SEGMENTS,
                true,
                0.0F
        );
        renderHead(
                matrix,
                buffer,
                comet.path(),
                progress,
                comet.size(),
                comet.headRotation() + (float) (worldTime * comet.spinSpeed()),
                comet.color(),
                Math.round(232.0F * alphaScale),
                true,
                0.0F
        );
    }

    private static void renderTrail(Matrix4f matrix, BufferBuilder buffer, SkyPath path, float headProgress,
                                    float trailProgressLength, float size, int color, int headAlpha,
                                    int segments, boolean comet, float skyRotationDegrees) {
        if (headAlpha <= 0) {
            return;
        }

        float tailProgress = Math.max(0.0F, headProgress - trailProgressLength);
        int outerColor = comet ? CelestialColorPalette.mix(color, 0xFFFFFF, 0.08F) : 0xF8FBFF;
        double skyRadians = Math.toRadians(skyRotationDegrees);
        TrailSample[] samples = buildTrailSamples(
                path,
                tailProgress,
                headProgress,
                size,
                headAlpha,
                segments,
                comet,
                Math.cos(skyRadians),
                Math.sin(skyRadians)
        );
        if (samples.length < 2) {
            return;
        }
        emitTrailPass(matrix, buffer, samples, outerColor, comet ? 1.14F : 1.38F, comet ? 0.11F : 0.18F);
        emitTrailPass(matrix, buffer, samples, color, 1.0F, 1.0F);
    }

    private static TrailSample[] buildTrailSamples(SkyPath path, float tailProgress, float headProgress, float size,
                                                   int headAlpha, int segments, boolean comet,
                                                   double skyCos, double skySin) {
        TrailSample[] samples = new TrailSample[segments + 1];
        Vec3d previousSide = null;
        for (int index = 0; index <= segments; index++) {
            float tailPosition = index / (float) segments;
            float pathProgress = MathHelper.lerp(tailPosition, tailProgress, headProgress);
            float tailFade = (float) Math.pow(tailPosition, comet ? 1.35D : 1.65D);
            float width = size * MathHelper.lerp(tailFade, comet ? 0.18F : 0.20F, comet ? 0.92F : 1.0F);
            int alpha = Math.round(headAlpha * tailFade * (comet ? 0.78F : 0.72F));

            Vec3d direction = CelestaraSkyGeometry.rotateX(path.positionAt(pathProgress), skyCos, skySin);
            Vec3d tangent = CelestaraSkyGeometry.rotateX(path.tangentAt(pathProgress), skyCos, skySin);
            Vec3d side = Vecs.safeNormalize(direction.crossProduct(tangent), previousSide == null ? Vec3d.ZERO : previousSide);
            if (side.lengthSquared() <= 1.0E-7D) {
                side = previousSide == null ? RIGHT : previousSide;
            }
            if (previousSide != null && side.dotProduct(previousSide) < 0.0D) {
                side = side.multiply(-1.0D);
            }

            samples[index] = new TrailSample(CelestaraSkyGeometry.skyPosition(direction), side, width, alpha);
            previousSide = side;
        }
        return samples;
    }

    private static void emitTrailPass(Matrix4f matrix, BufferBuilder buffer, TrailSample[] samples, int color,
                                      float widthMultiplier, float alphaMultiplier) {
        for (int index = 0; index < samples.length - 1; index++) {
            TrailSample start = samples[index];
            TrailSample end = samples[index + 1];
            int startAlpha = Math.round(start.alpha() * alphaMultiplier);
            int endAlpha = Math.round(end.alpha() * alphaMultiplier);
            if (startAlpha <= 0 && endAlpha <= 0) {
                continue;
            }

            Vec3d first = start.center().add(start.side().multiply(start.width() * widthMultiplier));
            Vec3d second = start.center().subtract(start.side().multiply(start.width() * widthMultiplier));
            Vec3d third = end.center().subtract(end.side().multiply(end.width() * widthMultiplier));
            Vec3d fourth = end.center().add(end.side().multiply(end.width() * widthMultiplier));
            CelestaraSkyGeometry.emitQuad(matrix, buffer, first, second, third, fourth, color, startAlpha, endAlpha);
        }
    }

    private static void renderHead(Matrix4f matrix, BufferBuilder buffer, SkyPath path, float progress, float size,
                                   float rotation, int color, int alpha, boolean comet, float skyRotationDegrees) {
        if (alpha <= 0) {
            return;
        }

        double skyRadians = Math.toRadians(skyRotationDegrees);
        double skyCos = Math.cos(skyRadians);
        double skySin = Math.sin(skyRadians);
        Vec3d direction = CelestaraSkyGeometry.rotateX(path.positionAt(progress), skyCos, skySin);
        Vec3d tangent = CelestaraSkyGeometry.rotateX(path.tangentAt(progress), skyCos, skySin);
        Vec3d right = Vecs.safeNormalize(tangent, RIGHT);
        Vec3d up = Vecs.safeNormalize(direction.crossProduct(right), UP);
        double radians = Math.toRadians(rotation);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vec3d spinRight = right.multiply(cos).add(up.multiply(sin));
        Vec3d spinUp = up.multiply(cos).subtract(right.multiply(sin));
        Vec3d center = CelestaraSkyGeometry.skyPosition(direction);

        if (comet) {
            int glowColor = CelestialColorPalette.mix(color, 0xFFFFFF, 0.10F);
            CelestaraSkyGeometry.emitPentagon(matrix, buffer, center, spinRight, spinUp, size * 1.55F, rotation,
                    glowColor, Math.round(alpha * 0.20F));
            CelestaraSkyGeometry.emitPentagon(matrix, buffer, center, spinRight, spinUp, size, rotation,
                    color, Math.round(alpha * 0.78F));
            CelestaraSkyGeometry.emitPentagon(matrix, buffer, center, spinRight, spinUp, size * 0.42F, rotation,
                    CelestialColorPalette.mix(color, 0xFFFFFF, 0.58F), alpha);
        } else {
            CelestaraSkyGeometry.emitPentagon(matrix, buffer, center, spinRight, spinUp, size * 1.65F, rotation,
                    0xF8FBFF, Math.round(alpha * 0.18F));
            CelestaraSkyGeometry.emitPentagon(matrix, buffer, center, spinRight, spinUp, size, rotation,
                    color, alpha);
        }
    }

    private static int upperBoundShootingStars(List<ShootingStarEvent> events, double nightOffset) {
        int low = 0;
        int high = events.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (events.get(middle).startOffset() <= nightOffset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private record TrailSample(Vec3d center, Vec3d side, float width, int alpha) {
    }
}
