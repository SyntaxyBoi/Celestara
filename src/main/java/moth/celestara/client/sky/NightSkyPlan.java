package moth.celestara.client.sky;

import net.minecraft.util.Identifier;

import java.util.List;

public record NightSkyPlan(
        Identifier dimension,
        long nightIndex,
        long seed,
        float starCountMultiplier,
        boolean meteorShower,
        boolean forcedMeteorShower,
        boolean forcedCommandComet,
        List<StarVisualData> stars,
        List<ShootingStarEvent> shootingStars,
        List<CometEvent> comets,
        int maxShootingStarDuration,
        int maxCometDuration
) {
}
