package moth.celestara.event;

public record PersistentCometSeed(
        long startTime,
        long seed,
        boolean commandForced,
        int colorOverride
) {
}
