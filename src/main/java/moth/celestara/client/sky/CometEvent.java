package moth.celestara.client.sky;

public record CometEvent(
        int startOffset,
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
    public int endOffset() {
        return startOffset + durationTicks;
    }

    public boolean isActive(double nightOffset) {
        return nightOffset >= startOffset && nightOffset <= endOffset();
    }

    public float progress(double nightOffset) {
        return (float) ((nightOffset - startOffset) / durationTicks);
    }
}
