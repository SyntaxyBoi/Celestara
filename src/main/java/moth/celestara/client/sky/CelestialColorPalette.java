package moth.celestara.client.sky;

import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class CelestialColorPalette {
    private static final int[] STAR_COLORS = {
            0xFFF8E8,
            0xFFF2C9,
            0xFFD9B0,
            0xFFE9D6,
            0xE8F4FF,
            0xD9E8FF,
            0xF0E7FF,
            0xFFCDBB
    };

    private static final int[] COMET_PURPLES = {
            0xA98BFF,
            0xBE95FF,
            0xD0A6FF,
            0xB9A1FF
    };

    private static final int[] COMET_BLUES = {
            0x92C8FF,
            0xA8D8FF,
            0x7FB7F2,
            0xB8E5FF
    };

    private static final int[] COMET_ORANGES = {
            0xFFB16D,
            0xFFC188,
            0xFF9E72,
            0xFFD0A3
    };

    private static final int[] PERSISTENT_COMET_COLORS = {
            0xA98BFF,
            0x92C8FF,
            0xB8E5FF,
            0x94FFD7,
            0xB6FF9B,
            0xFFD0F0,
            0xFFB16D,
            0xF7FFF2,
            0xD7F0FF,
            0xFFC6A8
    };

    private CelestialColorPalette() {
    }

    public static int star(Random random) {
        return star(random, CelestaraVisualConstants.STAR_COLOR_INTENSITY);
    }

    public static int star(Random random, float colorIntensity) {
        int base = STAR_COLORS[random.nextInt(STAR_COLORS.length)];
        float pullToWhite = 1.0F - MathHelper.clamp(colorIntensity, 0.0F, 1.0F);
        return mix(base, 0xFFFFFF, pullToWhite * range(random, 0.45F, 0.9F));
    }

    public static int supergiant(Random random) {
        float roll = random.nextFloat();
        if (roll < 0.50F) {
            return mix(0xFF765F, 0xFFFFFF, range(random, 0.02F, 0.10F));
        }
        if (roll < 0.80F) {
            return mix(0xFFE47A, 0xFFFFFF, range(random, 0.03F, 0.12F));
        }
        return mix(0x9CCBFF, 0xFFFFFF, range(random, 0.02F, 0.10F));
    }

    public static int shootingStar() {
        return 0xFFFDF7;
    }

    public static int comet(Random random) {
        int family = random.nextInt(3);
        int[] colors = family == 0 ? COMET_PURPLES : family == 1 ? COMET_BLUES : COMET_ORANGES;
        int color = colors[random.nextInt(colors.length)];
        return mix(color, 0xFFFFFF, range(random, 0.04F, 0.16F));
    }

    public static int persistentComet(Random random) {
        int color = PERSISTENT_COMET_COLORS[random.nextInt(PERSISTENT_COMET_COLORS.length)];
        return mix(color, 0xFFFFFF, range(random, 0.02F, 0.12F));
    }

    public static int scale(int color, float scale) {
        int red = MathHelper.clamp(Math.round(red(color) * scale), 0, 255);
        int green = MathHelper.clamp(Math.round(green(color) * scale), 0, 255);
        int blue = MathHelper.clamp(Math.round(blue(color) * scale), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    public static int mix(int first, int second, float delta) {
        float t = MathHelper.clamp(delta, 0.0F, 1.0F);
        int red = Math.round(MathHelper.lerp(t, red(first), red(second)));
        int green = Math.round(MathHelper.lerp(t, green(first), green(second)));
        int blue = Math.round(MathHelper.lerp(t, blue(first), blue(second)));
        return (red << 16) | (green << 8) | blue;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    private static float range(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
