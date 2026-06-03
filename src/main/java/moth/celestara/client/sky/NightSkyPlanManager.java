package moth.celestara.client.sky;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class NightSkyPlanManager {
    private static final Map<PlanKey, ForcedNightSkyOptions> FORCED_OPTIONS = new HashMap<>();
    private static NightSkyPlan currentPlan;

    private NightSkyPlanManager() {
    }

    public static NightSkyPlan getPlan(ClientWorld world) {
        if (!isAllowedDimension(world)) {
            currentPlan = null;
            return null;
        }

        int phase = dayPhase(world.getTimeOfDay());
        if (phase < CelestaraVisualConstants.NIGHT_START_TICK) {
            return currentPlan;
        }

        Identifier dimension = world.getRegistryKey().getValue();
        long nightIndex = nightIndexForTime(world.getTimeOfDay());
        if (currentPlan != null
                && currentPlan.nightIndex() == nightIndex
                && currentPlan.dimension().equals(dimension)) {
            return currentPlan;
        }

        PlanKey key = new PlanKey(dimension, nightIndex);
        ForcedNightSkyOptions options = FORCED_OPTIONS.getOrDefault(key, ForcedNightSkyOptions.NONE);
        currentPlan = NightSkyPlanGenerator.generate(world, nightIndex, options);
        return currentPlan;
    }

    public static void applyCommandOverride(Identifier dimension, long timeOfDay,
                                            ForcedNightEvent event, boolean enabled) {
        PlanKey key = new PlanKey(dimension, nightIndexForTime(timeOfDay));
        ForcedNightSkyOptions existing = FORCED_OPTIONS.getOrDefault(key, ForcedNightSkyOptions.NONE);
        ForcedNightSkyOptions updated;
        int offset = nightOffsetForTime(timeOfDay);

        if (event == ForcedNightEvent.METEOR_SHOWER) {
            updated = existing.withMeteorShower(enabled);
        } else if (enabled && existing.forceMeteorShower()) {
            updated = existing;
        } else {
            updated = existing.withCommandComet(enabled, offset);
        }

        if (updated.isEmpty()) {
            FORCED_OPTIONS.remove(key);
        } else {
            FORCED_OPTIONS.put(key, updated);
        }

        if (currentPlan != null
                && currentPlan.nightIndex() == key.nightIndex()
                && currentPlan.dimension().equals(key.dimension())) {
            currentPlan = null;
        }
    }

    public static void clear() {
        currentPlan = null;
        FORCED_OPTIONS.clear();
    }

    public static boolean isAllowedDimension(ClientWorld world) {
        return world != null
                && World.OVERWORLD.equals(world.getRegistryKey())
                && world.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.NORMAL;
    }

    public static double currentNightOffset(ClientWorld world, float tickDelta) {
        int phase = dayPhase(world.getTimeOfDay());
        if (phase < CelestaraVisualConstants.NIGHT_START_TICK) {
            return -1.0D;
        }
        return MathHelper.clamp(
                phase - CelestaraVisualConstants.NIGHT_START_TICK + tickDelta,
                0.0D,
                CelestaraVisualConstants.NIGHT_DURATION_TICKS
        );
    }

    public static long nightIndexForTime(long timeOfDay) {
        return Math.floorDiv(timeOfDay, CelestaraVisualConstants.DAY_LENGTH_TICKS);
    }

    public static int nightOffsetForTime(long timeOfDay) {
        int phase = dayPhase(timeOfDay);
        if (phase < CelestaraVisualConstants.NIGHT_START_TICK) {
            return 0;
        }
        return MathHelper.clamp(
                phase - CelestaraVisualConstants.NIGHT_START_TICK,
                0,
                CelestaraVisualConstants.NIGHT_DURATION_TICKS
        );
    }

    private static int dayPhase(long timeOfDay) {
        return (int) Math.floorMod(timeOfDay, CelestaraVisualConstants.DAY_LENGTH_TICKS);
    }

    private record PlanKey(Identifier dimension, long nightIndex) {
    }
}
