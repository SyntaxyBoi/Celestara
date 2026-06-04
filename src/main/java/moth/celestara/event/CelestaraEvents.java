package moth.celestara.event;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CelestaraEvents {
    public static final long DAY_LENGTH_TICKS = 24000L;
    public static final int NIGHT_START_TICK = 12000;
    public static final int SOLAR_VISIBLE_START_TICK = 23000;
    public static final int SOLAR_VISIBLE_END_TICK = 13000;
    public static final int LUNAR_VISIBLE_START_TICK = 11900;
    public static final int LUNAR_VISIBLE_END_TICK = 1000;
    public static final int SOLAR_COMMAND_TIME = 23000;
    public static final int LUNAR_COMMAND_TIME = 11950;

    public static final float SOLAR_ECLIPSE_CHANCE = 0.005F;
    public static final float LUNAR_ECLIPSE_CHANCE = 0.005F;
    public static final float PERSISTENT_COMET_CHANCE = 0.005F;
    public static final int PERSISTENT_COMET_DURATION_TICKS = (int) (DAY_LENGTH_TICKS * 5L);

    public static final long SOLAR_ECLIPSE_SALT = 0x5A1A4E51C1A55E11L;
    public static final long LUNAR_ECLIPSE_SALT = 0x1EAFB100D2E4C1A5L;
    public static final long PERSISTENT_COMET_ROLL_SALT = 0xC02E77A1A51E5D3BL;
    public static final long PERSISTENT_COMET_SHAPE_SALT = 0x9A771E5C04E71A5CL;

    private static final Set<EventKey> FORCED_SOLAR_ECLIPSES = new HashSet<>();
    private static final Set<EventKey> FORCED_LUNAR_ECLIPSES = new HashSet<>();
    private static final Map<Class<?>, Method> WORLD_SEED_METHODS = new HashMap<>();
    private static final Set<Class<?>> WORLD_SEED_METHOD_MISSES = new HashSet<>();

    private CelestaraEvents() {
    }

    public static long dayIndex(long timeOfDay) {
        return Math.floorDiv(timeOfDay, DAY_LENGTH_TICKS);
    }

    public static int dayPhase(long timeOfDay) {
        return (int) Math.floorMod(timeOfDay, DAY_LENGTH_TICKS);
    }

    public static boolean isNight(long timeOfDay) {
        return dayPhase(timeOfDay) >= NIGHT_START_TICK;
    }

    public static long visibleSolarEclipseDay(long timeOfDay) {
        int phase = dayPhase(timeOfDay);
        long dayIndex = dayIndex(timeOfDay);
        if (phase >= SOLAR_VISIBLE_START_TICK) {
            return dayIndex + 1L;
        }
        if (phase <= SOLAR_VISIBLE_END_TICK) {
            return dayIndex;
        }
        return Long.MIN_VALUE;
    }

    public static long visibleLunarEclipseNight(long timeOfDay) {
        int phase = dayPhase(timeOfDay);
        long dayIndex = dayIndex(timeOfDay);
        if (phase >= LUNAR_VISIBLE_START_TICK) {
            return dayIndex;
        }
        if (phase <= LUNAR_VISIBLE_END_TICK) {
            return dayIndex - 1L;
        }
        return Long.MIN_VALUE;
    }

    public static void forceSolarEclipse(Identifier dimension, long dayIndex) {
        FORCED_SOLAR_ECLIPSES.add(new EventKey(dimension, dayIndex));
    }

    public static void forceLunarEclipse(Identifier dimension, long nightIndex) {
        FORCED_LUNAR_ECLIPSES.add(new EventKey(dimension, nightIndex));
    }

    public static boolean isSolarEclipseForced(Identifier dimension, long dayIndex) {
        return FORCED_SOLAR_ECLIPSES.contains(new EventKey(dimension, dayIndex));
    }

    public static boolean isLunarEclipseForced(Identifier dimension, long nightIndex) {
        return FORCED_LUNAR_ECLIPSES.contains(new EventKey(dimension, nightIndex));
    }

    public static void clearForcedEclipses() {
        FORCED_SOLAR_ECLIPSES.clear();
        FORCED_LUNAR_ECLIPSES.clear();
    }

    public static boolean isSolarEclipseActive(World world) {
        if (!isAllowedWorld(world)) {
            return false;
        }

        long timeOfDay = world.getTimeOfDay();
        long eclipseDay = visibleSolarEclipseDay(timeOfDay);
        if (eclipseDay == Long.MIN_VALUE) {
            return false;
        }

        return isSolarEclipse(world, eclipseDay);
    }

    public static boolean isLunarEclipseActive(World world) {
        if (!isAllowedWorld(world)) {
            return false;
        }

        long timeOfDay = world.getTimeOfDay();
        long eclipseNight = visibleLunarEclipseNight(timeOfDay);
        if (eclipseNight == Long.MIN_VALUE) {
            return false;
        }

        return isLunarEclipse(world, eclipseNight);
    }

    public static boolean isSolarEclipse(World world, long dayIndex) {
        if (!isAllowedWorld(world)) {
            return false;
        }

        Identifier dimension = world.getRegistryKey().getValue();
        if (FORCED_SOLAR_ECLIPSES.contains(new EventKey(dimension, dayIndex))) {
            return true;
        }

        long seed = stableSeed(availableWorldSeed(world), dimension, dayIndex, SOLAR_ECLIPSE_SALT);
        return stableUnit(seed) < SOLAR_ECLIPSE_CHANCE;
    }

    public static boolean isLunarEclipse(World world, long nightIndex) {
        if (!isAllowedWorld(world)) {
            return false;
        }

        Identifier dimension = world.getRegistryKey().getValue();
        if (FORCED_LUNAR_ECLIPSES.contains(new EventKey(dimension, nightIndex))) {
            return true;
        }

        if (!isFullMoon(world, nightIndex)) {
            return false;
        }

        long seed = stableSeed(availableWorldSeed(world), dimension, nightIndex, LUNAR_ECLIPSE_SALT);
        return stableUnit(seed) < LUNAR_ECLIPSE_CHANCE;
    }

    public static boolean isFullMoon(World world, long nightIndex) {
        return world != null && world.getDimension().getMoonPhase(nightIndex * DAY_LENGTH_TICKS) == 0;
    }

    public static boolean isAllowedWorld(World world) {
        return world != null && World.OVERWORLD.equals(world.getRegistryKey());
    }

    public static long availableWorldSeed(World world) {
        if (world instanceof ServerWorld serverWorld) {
            return serverWorld.getSeed();
        }

        Class<?> worldClass = world.getClass();
        if (WORLD_SEED_METHOD_MISSES.contains(worldClass)) {
            return 0L;
        }

        Method method = WORLD_SEED_METHODS.get(worldClass);
        if (method == null) {
            try {
                method = worldClass.getMethod("getSeed");
                WORLD_SEED_METHODS.put(worldClass, method);
            } catch (NoSuchMethodException | SecurityException ignored) {
                WORLD_SEED_METHOD_MISSES.add(worldClass);
                return 0L;
            }
        }

        try {
            Object value = method.invoke(world);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Multiplayer clients often do not know the seed; event sync supplies authoritative rolls when available.
        }

        return 0L;
    }

    public static long stableSeed(long worldSeed, Identifier dimension, long index, long salt) {
        long seed = worldSeed ^ salt;
        seed ^= ((long) dimension.toString().hashCode()) * 0x9E3779B97F4A7C15L;
        seed ^= index * 0xD1B54A32D192ED03L;
        return mix(seed);
    }

    public static double stableUnit(long seed) {
        return (mix(seed) >>> 11) * 0x1.0p-53;
    }

    public static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }

    private record EventKey(Identifier dimension, long index) {
    }
}
