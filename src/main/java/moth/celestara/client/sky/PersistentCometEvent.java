package moth.celestara.client.sky;

import moth.celestara.event.CelestaraEvents;
import moth.celestara.event.PersistentCometSeed;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public record PersistentCometEvent(
        long startTime,
        int durationTicks,
        SkyPath path,
        float size,
        float brightness,
        float headRotation,
        float spinSpeed,
        float trailProgressLength,
        int color,
        boolean commandForced
) {
    public static PersistentCometEvent create(PersistentCometSeed seed) {
        Random random = new Random(seed.seed());
        return new PersistentCometEvent(
                seed.startTime(),
                CelestaraEvents.PERSISTENT_COMET_DURATION_TICKS,
                generatePath(random),
                range(random, CelestaraVisualConstants.PERSISTENT_COMET_MIN_SIZE, CelestaraVisualConstants.PERSISTENT_COMET_MAX_SIZE),
                range(random, CelestaraVisualConstants.PERSISTENT_COMET_MIN_BRIGHTNESS, CelestaraVisualConstants.PERSISTENT_COMET_MAX_BRIGHTNESS),
                range(random, 0.0F, 360.0F),
                range(random, 0.08F, 0.22F) * (random.nextBoolean() ? 1.0F : -1.0F),
                range(random, CelestaraVisualConstants.PERSISTENT_COMET_MIN_TRAIL_PROGRESS, CelestaraVisualConstants.PERSISTENT_COMET_MAX_TRAIL_PROGRESS),
                CometColorChoice.colorOrRandom(seed.colorOverride(), random, true),
                seed.commandForced()
        );
    }

    public boolean isActive(double worldTime) {
        return worldTime >= startTime && worldTime <= startTime + durationTicks;
    }

    public float progress(double worldTime) {
        return (float) ((worldTime - startTime) / durationTicks);
    }

    public float fade(double worldTime) {
        double age = worldTime - startTime;
        double remaining = startTime + durationTicks - worldTime;
        float fadeIn = (float) Math.min(1.0D, Math.max(0.0D, age / 1800.0D));
        float fadeOut = (float) Math.min(1.0D, Math.max(0.0D, remaining / 3200.0D));
        return Math.min(fadeIn, fadeOut);
    }

    private static SkyPath generatePath(Random random) {
        double startAngle = range(random, 0.0D, Math.PI * 2.0D);
        double direction = random.nextBoolean() ? 1.0D : -1.0D;
        double endAngle = startAngle + (Math.PI * 1.62D * direction) + range(random, -0.18D, 0.18D);
        double apexAngle = startAngle + (Math.PI * 0.78D * direction) + range(random, -0.26D, 0.26D);

        Vec3d start = directionFromAngleAndY(startAngle, -0.42D);
        Vec3d end = directionFromAngleAndY(endAngle, -0.38D);
        Vec3d control = directionFromAngleAndY(apexAngle, range(random, 0.82D, 0.98D));
        return new SkyPath(start, control, end);
    }

    private static Vec3d directionFromAngleAndY(double angle, double y) {
        double clampedY = Math.max(-0.995D, Math.min(0.995D, y));
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - clampedY * clampedY));
        return new Vec3d(Math.cos(angle) * horizontal, clampedY, Math.sin(angle) * horizontal);
    }

    private static float range(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static double range(Random random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
