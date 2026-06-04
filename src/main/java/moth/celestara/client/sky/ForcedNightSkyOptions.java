package moth.celestara.client.sky;

public record ForcedNightSkyOptions(
        boolean forceMeteorShower,
        boolean forceCommandComet,
        int commandStartOffset,
        int commandCometColor,
        NightSkyMood forcedMood
) {
    public static final ForcedNightSkyOptions NONE = new ForcedNightSkyOptions(
            false,
            false,
            0,
            CometColorChoice.RANDOM_COLOR,
            NightSkyMood.NORMAL
    );

    public ForcedNightSkyOptions withMeteorShower(boolean enabled) {
        return enabled
                ? new ForcedNightSkyOptions(true, false, commandStartOffset, commandCometColor, forcedMood)
                : new ForcedNightSkyOptions(false, forceCommandComet, commandStartOffset, commandCometColor, forcedMood);
    }

    public ForcedNightSkyOptions withCommandComet(boolean enabled, int startOffset, int colorOverride) {
        return new ForcedNightSkyOptions(
                forceMeteorShower,
                enabled,
                enabled ? startOffset : commandStartOffset,
                enabled ? colorOverride : commandCometColor,
                forcedMood
        );
    }

    public ForcedNightSkyOptions withForcedMood(NightSkyMood mood) {
        return new ForcedNightSkyOptions(forceMeteorShower, forceCommandComet, commandStartOffset, commandCometColor, mood);
    }

    public boolean isEmpty() {
        return !forceMeteorShower && !forceCommandComet && forcedMood == NightSkyMood.NORMAL;
    }
}
