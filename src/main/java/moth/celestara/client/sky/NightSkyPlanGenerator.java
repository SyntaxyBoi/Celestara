package moth.celestara.client.sky;

import moth.butterflyapi.math.Basis3;
import moth.butterflyapi.math.Scalars;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class NightSkyPlanGenerator {
    private NightSkyPlanGenerator() {
    }

    public static NightSkyPlan generate(ClientWorld world, long nightIndex, ForcedNightSkyOptions options) {
        Identifier dimension = world.getRegistryKey().getValue();
        long seed = stableSeed(world, dimension, nightIndex);
        Random planRandom = new Random(seed);
        boolean naturalMeteorShower = planRandom.nextFloat() < CelestaraVisualConstants.METEOR_SHOWER_CHANCE;
        boolean meteorShower = CelestaraVisualConstants.DEBUG_FORCE_METEOR_SHOWER || options.forceMeteorShower() || naturalMeteorShower;
        float starCountMultiplier = range(
                planRandom,
                CelestaraVisualConstants.MIN_STAR_COUNT_MULTIPLIER,
                CelestaraVisualConstants.MAX_STAR_COUNT_MULTIPLIER
        );
        int starLayerCount = CelestaraVisualConstants.STAR_LAYER_SPEEDS.length;
        int starCount = Math.max(starLayerCount, Math.round(CelestaraVisualConstants.VANILLA_STAR_COUNT * starCountMultiplier));
        starCount -= starCount % starLayerCount;

        List<StarVisualData> stars = generateStars(mix(seed ^ 0x51D1A7C2B6429D3BL), starCount);
        List<ShootingStarEvent> shootingStars = generateShootingStars(
                mix(seed ^ 0xA0F24A35CC91E2B7L),
                meteorShower
        );
        List<CometEvent> comets = meteorShower
                ? List.of()
                : generateComets(mix(seed ^ 0xC04E7A71D9B43F15L), options);

        int maxShootingDuration = shootingStars.stream()
                .mapToInt(ShootingStarEvent::durationTicks)
                .max()
                .orElse(0);
        int maxCometDuration = comets.stream()
                .mapToInt(CometEvent::durationTicks)
                .max()
                .orElse(0);

        return new NightSkyPlan(
                dimension,
                nightIndex,
                seed,
                starCountMultiplier,
                meteorShower,
                options.forceMeteorShower(),
                options.forceCommandComet() && !meteorShower,
                List.copyOf(stars),
                List.copyOf(shootingStars),
                List.copyOf(comets),
                maxShootingDuration,
                maxCometDuration
        );
    }

    private static List<StarVisualData> generateStars(long seed, int count) {
        Random random = new Random(seed);
        List<StarVisualData> stars = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Vec3d direction = randomStarDirection(random);
            int layer = index % CelestaraVisualConstants.STAR_LAYER_SPEEDS.length;
            float sizeRoll = random.nextFloat();
            float size = MathHelper.lerp(sizeRoll * sizeRoll, CelestaraVisualConstants.STAR_MIN_SIZE, CelestaraVisualConstants.STAR_MAX_SIZE);
            float brightness = MathHelper.clamp(
                    CelestaraVisualConstants.STAR_MIN_BRIGHTNESS
                            + (sizeRoll * 0.42F)
                            + (random.nextFloat() * 0.18F),
                    CelestaraVisualConstants.STAR_MIN_BRIGHTNESS,
                    CelestaraVisualConstants.STAR_MAX_BRIGHTNESS
            );
            float largeStarFactor = Scalars.clamp01((size - CelestaraVisualConstants.STAR_MIN_SIZE)
                    / (CelestaraVisualConstants.STAR_MAX_SIZE - CelestaraVisualConstants.STAR_MIN_SIZE));
            stars.add(new StarVisualData(
                    direction,
                    Basis3.fromForward(direction),
                    size,
                    brightness,
                    CelestialColorPalette.star(random),
                    range(random, 0.0F, 360.0F),
                    range(random, 0.0F, (float) (Math.PI * 2.0D)),
                    MathHelper.lerp(largeStarFactor, CelestaraVisualConstants.STAR_TWINKLE_MIN, CelestaraVisualConstants.STAR_TWINKLE_MAX),
                    range(random, 0.0065F, 0.023F),
                    range(random, 0.0F, (float) (Math.PI * 2.0D)),
                    range(random, 0.0018F, 0.0075F),
                    range(random, 2.0F, CelestaraVisualConstants.STAR_ROTATION_MAX_DEGREES),
                    CelestaraVisualConstants.STAR_LAYER_SPEEDS[layer]
            ));
        }
        return stars;
    }

    private static List<ShootingStarEvent> generateShootingStars(long seed, boolean meteorShower) {
        Random random = new Random(seed);
        int count = meteorShower
                ? CelestaraVisualConstants.METEOR_SHOWER_SHOOTING_STARS
                : CelestaraVisualConstants.NORMAL_SHOOTING_STARS_PER_NIGHT;
        int startMin = meteorShower ? 260 : 760;
        int startMax = meteorShower ? 11600 : 10950;
        double cell = (startMax - startMin) / (double) count;
        double showerStartAngle = range(random, 0.0D, Math.PI * 2.0D);
        double showerDirection = random.nextBoolean() ? 1.0D : -1.0D;
        List<ShootingStarEvent> events = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            int start = startMin + (int) Math.round((index + range(random, 0.08D, 0.92D)) * cell);
            int duration = randomInt(random, CelestaraVisualConstants.SHOOTING_STAR_MIN_DURATION, CelestaraVisualConstants.SHOOTING_STAR_MAX_DURATION);
            float showerSize = meteorShower ? range(random, 0.86F, CelestaraVisualConstants.SHOWER_SIZE_VARIATION) : range(random, 0.92F, 1.08F);
            float showerBrightness = meteorShower ? range(random, 0.82F, CelestaraVisualConstants.SHOWER_BRIGHTNESS_VARIATION) : range(random, 0.94F, 1.06F);
            float size = MathHelper.clamp(
                    range(random, CelestaraVisualConstants.SHOOTING_STAR_MIN_SIZE, CelestaraVisualConstants.SHOOTING_STAR_MAX_SIZE) * showerSize,
                    CelestaraVisualConstants.SHOOTING_STAR_MIN_SIZE * 0.72F,
                    CelestaraVisualConstants.SHOOTING_STAR_MAX_SIZE * 1.22F
            );
            float brightness = MathHelper.clamp(
                    range(random, CelestaraVisualConstants.SHOOTING_STAR_MIN_BRIGHTNESS, CelestaraVisualConstants.SHOOTING_STAR_MAX_BRIGHTNESS) * showerBrightness,
                    0.45F,
                    0.96F
            );
            events.add(new ShootingStarEvent(
                    start,
                    duration,
                    meteorShower
                            ? generatePath(random, false, showerStartAngle + range(random, -0.32D, 0.32D), showerDirection)
                            : generatePath(random, false),
                    size,
                    brightness,
                    range(random, 0.0F, 360.0F),
                    range(random, meteorShower ? 7.5F : 5.5F, meteorShower ? 16.5F : 12.0F) * (random.nextBoolean() ? 1.0F : -1.0F),
                    range(random, CelestaraVisualConstants.SHOOTING_STAR_MIN_TRAIL_PROGRESS, CelestaraVisualConstants.SHOOTING_STAR_MAX_TRAIL_PROGRESS),
                    CelestialColorPalette.shootingStar(),
                    meteorShower
            ));
        }

        events.sort(Comparator.comparingInt(ShootingStarEvent::startOffset));
        return events;
    }

    private static List<CometEvent> generateComets(long seed, ForcedNightSkyOptions options) {
        Random random = new Random(seed);
        List<CometEvent> comets = new ArrayList<>(3);
        float cometRoll = random.nextFloat();
        int naturalCount = 0;
        if (cometRoll < CelestaraVisualConstants.TWO_COMET_CHANCE) {
            naturalCount = 2;
        } else if (cometRoll < CelestaraVisualConstants.TWO_COMET_CHANCE + CelestaraVisualConstants.ONE_COMET_CHANCE) {
            naturalCount = 1;
        }

        if (naturalCount == 1) {
            comets.add(generateComet(random, randomInt(random, 1800, 9000), false));
        } else if (naturalCount == 2) {
            comets.add(generateComet(random, randomInt(random, 1200, 4300), false));
            comets.add(generateComet(random, randomInt(random, 6500, 10300), false));
        }

        if (CelestaraVisualConstants.DEBUG_FORCE_COMET || options.forceCommandComet()) {
            int offset = CelestaraVisualConstants.DEBUG_FORCE_COMET
                    ? 1000
                    : options.commandStartOffset();
            int delay = randomInt(random, CelestaraVisualConstants.FORCED_COMET_MIN_DELAY_TICKS, CelestaraVisualConstants.FORCED_COMET_MAX_DELAY_TICKS);
            CometEvent comet = generateComet(random, Math.max(0, offset + delay), true);
            int latestStart = Math.max(0, CelestaraVisualConstants.NIGHT_DURATION_TICKS - comet.durationTicks() - 40);
            comets.add(new CometEvent(
                    MathHelper.clamp(comet.startOffset(), 0, latestStart),
                    comet.durationTicks(),
                    comet.path(),
                    comet.size(),
                    comet.brightness(),
                    comet.headRotation(),
                    comet.spinSpeed(),
                    comet.trailProgressLength(),
                    comet.color(),
                    true
            ));
        }

        comets.sort(Comparator.comparingInt(CometEvent::startOffset));
        return comets;
    }

    private static CometEvent generateComet(Random random, int startOffset, boolean commandForced) {
        int duration = commandForced
                ? randomInt(random, CelestaraVisualConstants.FORCED_COMET_MIN_DURATION, CelestaraVisualConstants.FORCED_COMET_MAX_DURATION)
                : randomInt(random, CelestaraVisualConstants.COMET_MIN_DURATION, CelestaraVisualConstants.COMET_MAX_DURATION);
        int latestStart = Math.max(0, CelestaraVisualConstants.NIGHT_DURATION_TICKS - duration - 40);
        float size = commandForced
                ? range(random, CelestaraVisualConstants.FORCED_COMET_MIN_SIZE, CelestaraVisualConstants.FORCED_COMET_MAX_SIZE)
                : range(random, CelestaraVisualConstants.COMET_MIN_SIZE, CelestaraVisualConstants.COMET_MAX_SIZE);
        float brightness = commandForced
                ? 1.0F
                : range(random, CelestaraVisualConstants.COMET_MIN_BRIGHTNESS, CelestaraVisualConstants.COMET_MAX_BRIGHTNESS);
        float trailLength = commandForced
                ? range(random, 0.34F, 0.50F)
                : range(random, CelestaraVisualConstants.COMET_MIN_TRAIL_PROGRESS, CelestaraVisualConstants.COMET_MAX_TRAIL_PROGRESS);
        return new CometEvent(
                MathHelper.clamp(startOffset, 0, latestStart),
                duration,
                generatePath(random, true),
                size,
                brightness,
                range(random, 0.0F, 360.0F),
                range(random, 1.1F, 3.4F) * (random.nextBoolean() ? 1.0F : -1.0F),
                trailLength,
                CelestialColorPalette.comet(random),
                commandForced
        );
    }

    private static SkyPath generatePath(Random random, boolean comet) {
        double startAngle = range(random, 0.0D, Math.PI * 2.0D);
        double direction = random.nextBoolean() ? 1.0D : -1.0D;
        return generatePath(random, comet, startAngle, direction);
    }

    private static SkyPath generatePath(Random random, boolean comet, double startAngle, double direction) {
        double endAngle = startAngle + (Math.PI * direction) + range(random, -0.11D, 0.11D);
        double apexAngle = startAngle + (Math.PI * 0.5D * direction) + range(random, -0.30D, 0.30D);
        double apexY = range(random, comet ? 0.92D : 0.88D, 0.995D);

        Vec3d start = directionFromAngleAndY(startAngle, CelestaraVisualConstants.MOVING_EFFECT_RING_Y);
        Vec3d end = directionFromAngleAndY(endAngle, CelestaraVisualConstants.MOVING_EFFECT_RING_Y);
        Vec3d control = directionFromAngleAndY(apexAngle, apexY);
        return new SkyPath(start, control, end);
    }

    private static Vec3d randomStarDirection(Random random) {
        double y = range(random, -0.96D, 0.96D);
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double angle = range(random, 0.0D, Math.PI * 2.0D);
        return new Vec3d(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal);
    }

    private static Vec3d directionFromAngleAndY(double angle, double y) {
        double clampedY = Scalars.clamp(y, -0.995D, 0.995D);
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - clampedY * clampedY));
        return new Vec3d(Math.cos(angle) * horizontal, clampedY, Math.sin(angle) * horizontal);
    }

    private static long stableSeed(ClientWorld world, Identifier dimension, long nightIndex) {
        long seed = availableWorldSeed(world);
        seed ^= ((long) dimension.toString().hashCode()) * 0x9E3779B97F4A7C15L;
        seed ^= nightIndex * 0xD1B54A32D192ED03L;
        return mix(seed);
    }

    private static long availableWorldSeed(ClientWorld world) {
        try {
            Method method = world.getClass().getMethod("getSeed");
            Object value = method.invoke(world);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Multiplayer clients often do not know the world seed; dimension and night still keep the plan stable.
        }
        return 0L;
    }

    private static int randomInt(Random random, int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    private static float range(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static double range(Random random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }
}
