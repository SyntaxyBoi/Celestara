package moth.celestara.client.sky;

public final class CelestaraVisualConstants {
    public static final boolean ENHANCED_STARS_ENABLED = true;
    public static final boolean MOVING_EFFECTS_ENABLED = true;
    public static final boolean DEBUG_FORCE_METEOR_SHOWER = false;
    public static final boolean DEBUG_FORCE_COMET = false;

    public static final int VANILLA_STAR_COUNT = 1500;
    public static final float MIN_STAR_COUNT_MULTIPLIER = 0.80F;
    public static final float MAX_STAR_COUNT_MULTIPLIER = 1.50F;

    public static final long DAY_LENGTH_TICKS = 24000L;
    public static final int NIGHT_START_TICK = 12000;
    public static final int NIGHT_DURATION_TICKS = 12000;
    public static final int NIGHT_END_TICK = NIGHT_START_TICK + NIGHT_DURATION_TICKS;
    public static final float SKY_RADIUS = 100.0F;
    public static final double MOVING_EFFECT_RING_Y = -0.50D;
    public static final float MIN_VISIBILITY = 0.015F;

    public static final int NORMAL_SHOOTING_STARS_PER_NIGHT = 20;
    public static final float QUIET_NIGHT_CHANCE = 0.20F;
    public static final float LOUD_NIGHT_CHANCE = 0.10F;
    public static final float QUIET_NIGHT_TWINKLE_MULTIPLIER = 0.68F;
    public static final float LOUD_NIGHT_STAR_COUNT_MULTIPLIER = 1.50F;
    public static final float LOUD_NIGHT_STAR_MAX_SIZE_MULTIPLIER = 1.18F;
    public static final float LOUD_NIGHT_STAR_COLOR_INTENSITY = 0.84F;
    public static final float METEOR_SHOWER_CHANCE = 0.02F;
    public static final int METEOR_SHOWER_SHOOTING_STARS = 200;
    public static final float ONE_COMET_CHANCE = 0.60F;
    public static final float TWO_COMET_CHANCE = 0.05F;

    public static final float STAR_MIN_SIZE = 0.035F;
    public static final float STAR_MAX_SIZE = 0.145F;
    public static final float STAR_MIN_BRIGHTNESS = 0.20F;
    public static final float STAR_MAX_BRIGHTNESS = 0.78F;
    public static final float STAR_COLOR_INTENSITY = 0.72F;
    public static final float STAR_TWINKLE_MIN = 0.025F;
    public static final float STAR_TWINKLE_MAX = 0.15F;
    public static final float STAR_ROTATION_MAX_DEGREES = 18.0F;
    public static final float[] STAR_LAYER_SPEEDS = {0.90F, 0.75F, 0.60F, 0.50F};
    public static final int SUPERGIANT_MIN_COUNT = 5;
    public static final int SUPERGIANT_MAX_COUNT = 30;
    public static final float SUPERGIANT_MIN_SIZE_MULTIPLIER = 1.55F;
    public static final float SUPERGIANT_MAX_SIZE_MULTIPLIER = 2.35F;
    public static final float SUPERGIANT_MIN_BRIGHTNESS = 0.72F;
    public static final float SUPERGIANT_MAX_BRIGHTNESS = 0.98F;
    public static final float LOUD_NIGHT_SUPERGIANT_MULTIPLIER = 1.30F;
    public static final float QUIET_NIGHT_SUPERGIANT_MULTIPLIER = 0.60F;

    public static final float SHOOTING_STAR_MIN_SIZE = 0.36F;
    public static final float SHOOTING_STAR_MAX_SIZE = 0.56F;
    public static final float SHOOTING_STAR_MIN_BRIGHTNESS = 0.72F;
    public static final float SHOOTING_STAR_MAX_BRIGHTNESS = 0.98F;
    public static final int SHOOTING_STAR_MIN_DURATION = 92;
    public static final int SHOOTING_STAR_MAX_DURATION = 168;
    public static final float SHOOTING_STAR_MIN_TRAIL_PROGRESS = 0.18F;
    public static final float SHOOTING_STAR_MAX_TRAIL_PROGRESS = 0.32F;

    public static final float SHOWER_SIZE_VARIATION = 1.26F;
    public static final float SHOWER_BRIGHTNESS_VARIATION = 1.18F;

    public static final float COMET_MIN_SIZE = 0.52F;
    public static final float COMET_MAX_SIZE = 0.78F;
    public static final int COMET_MIN_DURATION = 184;
    public static final int COMET_MAX_DURATION = 336;
    public static final float COMET_MIN_TRAIL_PROGRESS = 0.28F;
    public static final float COMET_MAX_TRAIL_PROGRESS = 0.44F;
    public static final float COMET_MIN_BRIGHTNESS = 0.84F;
    public static final float COMET_MAX_BRIGHTNESS = 1.0F;
    public static final int FORCED_COMET_MIN_DELAY_TICKS = 20;
    public static final int FORCED_COMET_MAX_DELAY_TICKS = 80;
    public static final int FORCED_COMET_MIN_DURATION = 220;
    public static final int FORCED_COMET_MAX_DURATION = 380;
    public static final float FORCED_COMET_MIN_SIZE = 0.82F;
    public static final float FORCED_COMET_MAX_SIZE = 1.10F;
    public static final float PERSISTENT_COMET_MIN_SIZE = COMET_MIN_SIZE * 1.15F;
    public static final float PERSISTENT_COMET_MAX_SIZE = COMET_MAX_SIZE * 1.15F;
    public static final float PERSISTENT_COMET_MIN_BRIGHTNESS = 0.92F;
    public static final float PERSISTENT_COMET_MAX_BRIGHTNESS = 1.0F;
    public static final float PERSISTENT_COMET_MIN_TRAIL_PROGRESS = 0.34F;
    public static final float PERSISTENT_COMET_MAX_TRAIL_PROGRESS = 0.52F;

    public static final int TRAIL_SEGMENTS = 14;
    public static final int COMET_TRAIL_SEGMENTS = 18;
    public static final int PERSISTENT_COMET_TRAIL_SEGMENTS = 24;
    public static final int HEAD_PENTAGON_POINTS = 5;

    private CelestaraVisualConstants() {
    }
}
