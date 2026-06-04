package moth.celestara.client.sky;

import moth.celestara.event.CelestaraEvents;
import moth.butterflyapi.math.Basis3;
import moth.butterflyapi.math.Scalars;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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
        NightSkyMood naturalMood = rollNightMood(planRandom.nextFloat());
        NightSkyMood mood = options.forcedMood() == NightSkyMood.NORMAL ? naturalMood : options.forcedMood();
        boolean quietNight = mood == NightSkyMood.QUIET;
        boolean loudNight = mood == NightSkyMood.LOUD;

        boolean naturalMeteorShower = planRandom.nextFloat() < CelestaraVisualConstants.METEOR_SHOWER_CHANCE;
        boolean forceMeteorShower = CelestaraVisualConstants.DEBUG_FORCE_METEOR_SHOWER || options.forceMeteorShower();
        boolean meteorShower = forceMeteorShower || (!quietNight && naturalMeteorShower);
        float starCountMultiplier = range(
                planRandom,
                CelestaraVisualConstants.MIN_STAR_COUNT_MULTIPLIER,
                CelestaraVisualConstants.MAX_STAR_COUNT_MULTIPLIER
        );
        if (loudNight) {
            starCountMultiplier *= CelestaraVisualConstants.LOUD_NIGHT_STAR_COUNT_MULTIPLIER;
        }
        int starLayerCount = CelestaraVisualConstants.STAR_LAYER_SPEEDS.length;
        int starCount = Math.max(starLayerCount, Math.round(CelestaraVisualConstants.VANILLA_STAR_COUNT * starCountMultiplier));
        starCount -= starCount % starLayerCount;
        int supergiantCount = scaledSupergiantCount(planRandom, mood);

        List<StarVisualData> stars = generateStars(mix(seed ^ 0x51D1A7C2B6429D3BL), starCount, mood);
        stars.addAll(generateSupergiants(mix(seed ^ 0x3F6E2DDE8370A5C1L), supergiantCount, mood));
        List<ShootingStarEvent> shootingStars = quietNight && !forceMeteorShower
                ? List.of()
                : generateShootingStars(
                        mix(seed ^ 0xA0F24A35CC91E2B7L),
                        meteorShower
                );
        boolean forceCommandComet = CelestaraVisualConstants.DEBUG_FORCE_COMET || options.forceCommandComet();
        boolean suppressNaturalComets = quietNight || (meteorShower && !loudNight);
        List<CometEvent> comets = suppressNaturalComets && !forceCommandComet
                ? List.of()
                : generateComets(mix(seed ^ 0xC04E7A71D9B43F15L), options, loudNight, suppressNaturalComets);

        int maxShootingDuration = 0;
        for (ShootingStarEvent event : shootingStars) {
            maxShootingDuration = Math.max(maxShootingDuration, event.durationTicks());
        }
        int maxCometDuration = 0;
        for (CometEvent comet : comets) {
            maxCometDuration = Math.max(maxCometDuration, comet.durationTicks());
        }

        return new NightSkyPlan(
                dimension,
                nightIndex,
                seed,
                starCountMultiplier,
                mood,
                options.forcedMood() != NightSkyMood.NORMAL,
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

    private static List<StarVisualData> generateStars(long seed, int count, NightSkyMood mood) {
        Random random = new Random(seed);
        List<StarVisualData> stars = new ArrayList<>(count);
        float maxSize = mood == NightSkyMood.LOUD
                ? CelestaraVisualConstants.STAR_MAX_SIZE * CelestaraVisualConstants.LOUD_NIGHT_STAR_MAX_SIZE_MULTIPLIER
                : CelestaraVisualConstants.STAR_MAX_SIZE;
        float colorIntensity = mood == NightSkyMood.LOUD
                ? CelestaraVisualConstants.LOUD_NIGHT_STAR_COLOR_INTENSITY
                : CelestaraVisualConstants.STAR_COLOR_INTENSITY;
        float twinkleMultiplier = mood == NightSkyMood.QUIET
                ? CelestaraVisualConstants.QUIET_NIGHT_TWINKLE_MULTIPLIER
                : 1.0F;

        for (int index = 0; index < count; index++) {
            Vec3d direction = randomStarDirection(random);
            int layer = index % CelestaraVisualConstants.STAR_LAYER_SPEEDS.length;
            float sizeRoll = random.nextFloat();
            float size = MathHelper.lerp(sizeRoll * sizeRoll, CelestaraVisualConstants.STAR_MIN_SIZE, maxSize);
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
                    CelestialColorPalette.star(random, colorIntensity),
                    range(random, 0.0F, 360.0F),
                    range(random, 0.0F, (float) (Math.PI * 2.0D)),
                    MathHelper.lerp(largeStarFactor, CelestaraVisualConstants.STAR_TWINKLE_MIN, CelestaraVisualConstants.STAR_TWINKLE_MAX)
                            * twinkleMultiplier,
                    range(random, 0.0065F, 0.023F),
                    range(random, 0.0F, (float) (Math.PI * 2.0D)),
                    range(random, 0.0018F, 0.0075F),
                    range(random, 2.0F, CelestaraVisualConstants.STAR_ROTATION_MAX_DEGREES),
                    CelestaraVisualConstants.STAR_LAYER_SPEEDS[layer],
                    false
            ));
        }
        return stars;
    }

    private static List<StarVisualData> generateSupergiants(long seed, int count, NightSkyMood mood) {
        Random random = new Random(seed);
        List<StarVisualData> stars = new ArrayList<>(count);
        float twinkleMultiplier = mood == NightSkyMood.QUIET
                ? CelestaraVisualConstants.QUIET_NIGHT_TWINKLE_MULTIPLIER
                : 1.0F;
        float minSize = CelestaraVisualConstants.STAR_MAX_SIZE * CelestaraVisualConstants.SUPERGIANT_MIN_SIZE_MULTIPLIER;
        float maxSize = CelestaraVisualConstants.STAR_MAX_SIZE * CelestaraVisualConstants.SUPERGIANT_MAX_SIZE_MULTIPLIER;

        for (int index = 0; index < count; index++) {
            Vec3d direction = randomStarDirection(random);
            int layer = index % CelestaraVisualConstants.STAR_LAYER_SPEEDS.length;
            float sizeRoll = random.nextFloat();
            float size = MathHelper.lerp(sizeRoll * sizeRoll, minSize, maxSize);
            float largeStarFactor = Scalars.clamp01((size - minSize) / (maxSize - minSize));
            stars.add(new StarVisualData(
                    direction,
                    Basis3.fromForward(direction),
                    size,
                    range(random, CelestaraVisualConstants.SUPERGIANT_MIN_BRIGHTNESS, CelestaraVisualConstants.SUPERGIANT_MAX_BRIGHTNESS),
                    CelestialColorPalette.supergiant(random),
                    range(random, 0.0F, 360.0F),
                    range(random, 0.0F, (float) (Math.PI * 2.0D)),
                    MathHelper.lerp(largeStarFactor, CelestaraVisualConstants.STAR_TWINKLE_MIN, CelestaraVisualConstants.STAR_TWINKLE_MAX)
                            * twinkleMultiplier,
                    range(random, 0.0045F, 0.017F),
                    range(random, 0.0F, (float) (Math.PI * 2.0D)),
                    range(random, 0.0012F, 0.0055F),
                    range(random, 2.0F, CelestaraVisualConstants.STAR_ROTATION_MAX_DEGREES),
                    CelestaraVisualConstants.STAR_LAYER_SPEEDS[layer],
                    true
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

    private static List<CometEvent> generateComets(long seed, ForcedNightSkyOptions options,
                                                   boolean loudNight, boolean suppressNaturalComets) {
        Random random = new Random(seed);
        List<CometEvent> comets = new ArrayList<>(4);
        int naturalCount = 0;
        if (!suppressNaturalComets) {
            if (loudNight) {
                naturalCount = randomInt(random, 2, 3);
            } else {
                float cometRoll = random.nextFloat();
                if (cometRoll < CelestaraVisualConstants.TWO_COMET_CHANCE) {
                    naturalCount = 2;
                } else if (cometRoll < CelestaraVisualConstants.TWO_COMET_CHANCE + CelestaraVisualConstants.ONE_COMET_CHANCE) {
                    naturalCount = 1;
                }
            }
        }

        if (naturalCount == 1) {
            comets.add(generateComet(random, randomInt(random, 1800, 9000), false, CometColorChoice.RANDOM_COLOR));
        } else if (naturalCount == 2) {
            comets.add(generateComet(random, randomInt(random, 1200, 4300), false, CometColorChoice.RANDOM_COLOR));
            comets.add(generateComet(random, randomInt(random, 6500, 10300), false, CometColorChoice.RANDOM_COLOR));
        } else if (naturalCount == 3) {
            comets.add(generateComet(random, randomInt(random, 900, 3000), false, CometColorChoice.RANDOM_COLOR));
            comets.add(generateComet(random, randomInt(random, 4300, 6900), false, CometColorChoice.RANDOM_COLOR));
            comets.add(generateComet(random, randomInt(random, 7800, 10800), false, CometColorChoice.RANDOM_COLOR));
        }

        if (CelestaraVisualConstants.DEBUG_FORCE_COMET || options.forceCommandComet()) {
            int offset = CelestaraVisualConstants.DEBUG_FORCE_COMET
                    ? 1000
                    : options.commandStartOffset();
            int delay = randomInt(random, CelestaraVisualConstants.FORCED_COMET_MIN_DELAY_TICKS, CelestaraVisualConstants.FORCED_COMET_MAX_DELAY_TICKS);
            CometEvent comet = generateComet(random, Math.max(0, offset + delay), true, options.commandCometColor());
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

    private static NightSkyMood rollNightMood(float roll) {
        if (roll < CelestaraVisualConstants.QUIET_NIGHT_CHANCE) {
            return NightSkyMood.QUIET;
        }
        if (roll < CelestaraVisualConstants.QUIET_NIGHT_CHANCE + CelestaraVisualConstants.LOUD_NIGHT_CHANCE) {
            return NightSkyMood.LOUD;
        }
        return NightSkyMood.NORMAL;
    }

    private static int scaledSupergiantCount(Random random, NightSkyMood mood) {
        int baseCount = randomInt(
                random,
                CelestaraVisualConstants.SUPERGIANT_MIN_COUNT,
                CelestaraVisualConstants.SUPERGIANT_MAX_COUNT
        );
        if (mood == NightSkyMood.LOUD) {
            return Math.round(baseCount * CelestaraVisualConstants.LOUD_NIGHT_SUPERGIANT_MULTIPLIER);
        }
        if (mood == NightSkyMood.QUIET) {
            return Math.round(baseCount * CelestaraVisualConstants.QUIET_NIGHT_SUPERGIANT_MULTIPLIER);
        }
        return baseCount;
    }

    private static CometEvent generateComet(Random random, int startOffset, boolean commandForced, int colorOverride) {
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
                CometColorChoice.colorOrRandom(colorOverride, random, false),
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
        long seed = CelestaraEvents.availableWorldSeed(world);
        seed ^= ((long) dimension.toString().hashCode()) * 0x9E3779B97F4A7C15L;
        seed ^= nightIndex * 0xD1B54A32D192ED03L;
        return mix(seed);
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
