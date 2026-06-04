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

        return getPlan(world, nightIndexForTime(world.getTimeOfDay()));
    }

    public static NightSkyPlan getPlan(ClientWorld world, long nightIndex) {
        if (!isAllowedDimension(world)) {
            currentPlan = null;
            return null;
        }

        Identifier dimension = world.getRegistryKey().getValue();
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
        applyCommandOverride(dimension, timeOfDay, event, enabled, CometColorChoice.RANDOM_COLOR);
    }

    public static void applyCommandOverride(Identifier dimension, long timeOfDay,
                                            ForcedNightEvent event, boolean enabled, int colorOverride) {
        PlanKey key = new PlanKey(dimension, nightIndexForTime(timeOfDay));
        if (event == ForcedNightEvent.REFRESH_SKY) {
            refresh(dimension, timeOfDay);
            return;
        }

        ForcedNightSkyOptions existing = FORCED_OPTIONS.getOrDefault(key, ForcedNightSkyOptions.NONE);
        ForcedNightSkyOptions updated;
        int offset = nightOffsetForTime(timeOfDay);

        switch (event) {
            case METEOR_SHOWER -> updated = existing.withMeteorShower(enabled);
            case COMET -> updated = enabled && existing.forceMeteorShower()
                    ? existing
                    : existing.withCommandComet(enabled, offset, colorOverride);
            case QUIET_NIGHT -> updated = existing.withForcedMood(enabled ? NightSkyMood.QUIET : NightSkyMood.NORMAL);
            case LOUD_NIGHT -> updated = existing.withForcedMood(enabled ? NightSkyMood.LOUD : NightSkyMood.NORMAL);
            case CLEAR_NIGHT_MOOD -> updated = existing.withForcedMood(NightSkyMood.NORMAL);
            default -> updated = existing;
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

    public static boolean refresh(Identifier dimension, long timeOfDay) {
        if (currentPlan == null) {
            return false;
        }

        long nightIndex = nightIndexForTime(timeOfDay);
        if (currentPlan.nightIndex() == nightIndex && currentPlan.dimension().equals(dimension)) {
            currentPlan = null;
            return true;
        }
        return false;
    }

    public static void clear() {
        currentPlan = null;
        FORCED_OPTIONS.clear();
    }

    public static NightSkyPlan currentPlan() {
        return currentPlan;
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
