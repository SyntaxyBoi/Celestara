package moth.celestara.client.sky;

import moth.celestara.event.CelestaraEvents;
import moth.celestara.event.PersistentCometSeed;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CelestaraClientEventState {
    private static final Set<Identifier> SERVER_SYNCED_DIMENSIONS = new HashSet<>();
    private static final Map<Identifier, Map<Long, Boolean>> SERVER_SOLAR_ECLIPSES = new HashMap<>();
    private static final Map<Identifier, Map<Long, Boolean>> SERVER_LUNAR_ECLIPSES = new HashMap<>();

    private CelestaraClientEventState() {
    }

    public static void applyServerSnapshot(Identifier dimension,
                                           Map<Long, Boolean> solarEclipses,
                                           Map<Long, Boolean> lunarEclipses,
                                           List<PersistentCometSeed> persistentComets) {
        SERVER_SYNCED_DIMENSIONS.add(dimension);
        SERVER_SOLAR_ECLIPSES.put(dimension, new HashMap<>(solarEclipses));
        SERVER_LUNAR_ECLIPSES.put(dimension, new HashMap<>(lunarEclipses));
        PersistentCometManager.applyServerSnapshot(dimension, persistentComets);
    }

    public static boolean isSolarEclipseActive(ClientWorld world) {
        if (!isAllowedWorld(world)) {
            return false;
        }

        long eclipseDay = CelestaraEvents.visibleSolarEclipseDay(world.getTimeOfDay());
        if (eclipseDay == Long.MIN_VALUE) {
            return false;
        }

        return isSolarEclipse(world, eclipseDay);
    }

    public static boolean isLunarEclipseActive(ClientWorld world) {
        if (!isAllowedWorld(world)) {
            return false;
        }

        long eclipseNight = CelestaraEvents.visibleLunarEclipseNight(world.getTimeOfDay());
        if (eclipseNight == Long.MIN_VALUE) {
            return false;
        }

        return isLunarEclipse(world, eclipseNight);
    }

    public static boolean isSolarEclipse(ClientWorld world, long dayIndex) {
        Identifier dimension = world.getRegistryKey().getValue();
        if (CelestaraEvents.isSolarEclipseForced(dimension, dayIndex)) {
            return true;
        }

        if (SERVER_SYNCED_DIMENSIONS.contains(dimension)) {
            return SERVER_SOLAR_ECLIPSES.getOrDefault(dimension, Map.of()).getOrDefault(dayIndex, false);
        }

        return CelestaraEvents.isSolarEclipse(world, dayIndex);
    }

    public static boolean isLunarEclipse(ClientWorld world, long nightIndex) {
        Identifier dimension = world.getRegistryKey().getValue();
        if (CelestaraEvents.isLunarEclipseForced(dimension, nightIndex)) {
            return true;
        }

        if (SERVER_SYNCED_DIMENSIONS.contains(dimension)) {
            return SERVER_LUNAR_ECLIPSES.getOrDefault(dimension, Map.of()).getOrDefault(nightIndex, false);
        }

        return CelestaraEvents.isLunarEclipse(world, nightIndex);
    }

    public static boolean hasServerSync(ClientWorld world) {
        return world != null && SERVER_SYNCED_DIMENSIONS.contains(world.getRegistryKey().getValue());
    }

    public static float solarEclipseFade(ClientWorld world) {
        if (!isAllowedWorld(world)) {
            return 0.0F;
        }

        return isSolarEclipseActive(world) ? 1.0F : 0.0F;
    }

    public static float lunarEclipseFade(ClientWorld world) {
        if (!isAllowedWorld(world) || !isLunarEclipseActive(world)) {
            return 0.0F;
        }

        int phase = CelestaraEvents.dayPhase(world.getTimeOfDay());
        int fadeTicks = 600;
        if (phase >= CelestaraEvents.LUNAR_VISIBLE_START_TICK) {
            return Math.min(1.0F, Math.max(0.0F, (phase - CelestaraEvents.LUNAR_VISIBLE_START_TICK) / (float) fadeTicks));
        }

        return Math.min(1.0F, Math.max(0.0F, (CelestaraEvents.LUNAR_VISIBLE_END_TICK - phase) / (float) fadeTicks));
    }

    public static void clear() {
        SERVER_SYNCED_DIMENSIONS.clear();
        SERVER_SOLAR_ECLIPSES.clear();
        SERVER_LUNAR_ECLIPSES.clear();
        PersistentCometManager.clear();
        CelestaraEvents.clearForcedEclipses();
    }

    private static boolean isAllowedWorld(ClientWorld world) {
        return world != null && NightSkyPlanManager.isAllowedDimension(world);
    }
}
