package moth.celestara.client.sky;

public record ForcedNightSkyOptions(
        boolean forceMeteorShower,
        boolean forceCommandComet,
        int commandStartOffset
) {
    public static final ForcedNightSkyOptions NONE = new ForcedNightSkyOptions(false, false, 0);

    public ForcedNightSkyOptions withMeteorShower(boolean enabled) {
        return enabled
                ? new ForcedNightSkyOptions(true, false, commandStartOffset)
                : new ForcedNightSkyOptions(false, forceCommandComet, commandStartOffset);
    }

    public ForcedNightSkyOptions withCommandComet(boolean enabled, int startOffset) {
        return new ForcedNightSkyOptions(forceMeteorShower, enabled, enabled ? startOffset : commandStartOffset);
    }

    public boolean isEmpty() {
        return !forceMeteorShower && !forceCommandComet;
    }
}
