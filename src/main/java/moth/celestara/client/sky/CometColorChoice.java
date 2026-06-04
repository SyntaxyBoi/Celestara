package moth.celestara.client.sky;

import java.util.Locale;
import java.util.Random;

public enum CometColorChoice {
    RANDOM("random", -1),
    PURPLE("purple", 0xA98BFF),
    BLUE("blue", 0x92C8FF),
    CYAN("cyan", 0x8FEAFF),
    GREEN("green", 0x94FFD7),
    PINK("pink", 0xFFD0F0),
    ORANGE("orange", 0xFFB16D),
    RED("red", 0xFF8A72),
    YELLOW("yellow", 0xFFE28A),
    WHITE("white", 0xF7FFF2);

    public static final int RANDOM_COLOR = -1;

    private final String id;
    private final int color;

    CometColorChoice(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    public static CometColorChoice byName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (CometColorChoice choice : values()) {
            if (choice.id.equals(normalized)) {
                return choice;
            }
        }
        return null;
    }

    public static String names() {
        StringBuilder builder = new StringBuilder();
        for (CometColorChoice choice : values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(choice.id);
        }
        return builder.toString();
    }

    public static int colorOrRandom(int colorOverride, Random random, boolean persistent) {
        if (colorOverride != RANDOM_COLOR) {
            return colorOverride;
        }
        return persistent
                ? CelestialColorPalette.persistentComet(random)
                : CelestialColorPalette.comet(random);
    }
}
