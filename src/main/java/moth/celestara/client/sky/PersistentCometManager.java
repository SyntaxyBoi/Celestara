package moth.celestara.client.sky;

import moth.celestara.event.CelestaraEvents;
import moth.celestara.event.PersistentCometSeed;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class PersistentCometManager {
    private static final Map<Identifier, List<PersistentCometEvent>> SERVER_COMETS = new HashMap<>();
    private static final Map<Identifier, List<PersistentCometEvent>> LOCAL_FORCED_COMETS = new HashMap<>();
    private static final Map<Identifier, NaturalCometCache> LOCAL_NATURAL_COMETS = new HashMap<>();

    private PersistentCometManager() {
    }

    public static void applyServerSnapshot(Identifier dimension, List<PersistentCometSeed> seeds) {
        List<PersistentCometEvent> events = new ArrayList<>(seeds.size());
        for (PersistentCometSeed seed : seeds) {
            events.add(PersistentCometEvent.create(seed));
        }
        SERVER_COMETS.put(dimension, events);
    }

    public static void spawnCommandComet(ClientWorld world) {
        spawnCommandComet(world, CometColorChoice.RANDOM_COLOR);
    }

    public static void spawnCommandComet(ClientWorld world, int colorOverride) {
        Identifier dimension = world.getRegistryKey().getValue();
        long startTime = world.getTimeOfDay();
        long seed = CelestaraEvents.stableSeed(
                CelestaraEvents.availableWorldSeed(world),
                dimension,
                new Random().nextLong() ^ startTime,
                CelestaraEvents.PERSISTENT_COMET_SHAPE_SALT
        );
        LOCAL_FORCED_COMETS
                .computeIfAbsent(dimension, ignored -> new ArrayList<>())
                .add(PersistentCometEvent.create(new PersistentCometSeed(startTime, seed, true, colorOverride)));
    }

    public static List<PersistentCometEvent> activeComets(ClientWorld world, float tickDelta) {
        Identifier dimension = world.getRegistryKey().getValue();
        double time = world.getTimeOfDay() + tickDelta;
        List<PersistentCometEvent> active = new ArrayList<>();

        if (CelestaraClientEventState.hasServerSync(world)) {
            addActive(active, SERVER_COMETS.getOrDefault(dimension, List.of()), time);
        } else {
            addActive(active, localNaturalComets(world), time);
        }

        List<PersistentCometEvent> forced = LOCAL_FORCED_COMETS.getOrDefault(dimension, List.of());
        addActive(active, forced, time);
        pruneForced(dimension, time);
        return active;
    }

    public static void clear() {
        SERVER_COMETS.clear();
        LOCAL_FORCED_COMETS.clear();
        LOCAL_NATURAL_COMETS.clear();
    }

    private static List<PersistentCometEvent> localNaturalComets(ClientWorld world) {
        Identifier dimension = world.getRegistryKey().getValue();
        long time = world.getTimeOfDay();
        long currentDay = CelestaraEvents.dayIndex(time);
        long worldSeed = CelestaraEvents.availableWorldSeed(world);
        NaturalCometCache cache = LOCAL_NATURAL_COMETS.get(dimension);
        if (cache != null && cache.dayIndex() == currentDay && cache.worldSeed() == worldSeed) {
            return cache.comets();
        }

        List<PersistentCometEvent> comets = new ArrayList<>(6);
        for (long nightIndex = currentDay - 5L; nightIndex <= currentDay; nightIndex++) {
            long startTime = nightIndex * CelestaraEvents.DAY_LENGTH_TICKS + CelestaraEvents.NIGHT_START_TICK;
            long rollSeed = CelestaraEvents.stableSeed(
                    worldSeed,
                    dimension,
                    nightIndex,
                    CelestaraEvents.PERSISTENT_COMET_ROLL_SALT
            );
            if (CelestaraEvents.stableUnit(rollSeed) < CelestaraEvents.PERSISTENT_COMET_CHANCE) {
                long shapeSeed = CelestaraEvents.stableSeed(
                        worldSeed,
                        dimension,
                        nightIndex,
                        CelestaraEvents.PERSISTENT_COMET_SHAPE_SALT
                );
                comets.add(PersistentCometEvent.create(new PersistentCometSeed(
                        startTime,
                        shapeSeed,
                        false,
                        CometColorChoice.RANDOM_COLOR
                )));
            }
        }

        List<PersistentCometEvent> cachedComets = List.copyOf(comets);
        LOCAL_NATURAL_COMETS.put(dimension, new NaturalCometCache(currentDay, worldSeed, cachedComets));
        return cachedComets;
    }

    private static void addActive(List<PersistentCometEvent> output, List<PersistentCometEvent> comets, double time) {
        for (PersistentCometEvent comet : comets) {
            if (comet.isActive(time)) {
                output.add(comet);
            }
        }
    }

    private static void pruneForced(Identifier dimension, double time) {
        List<PersistentCometEvent> forced = LOCAL_FORCED_COMETS.get(dimension);
        if (forced == null) {
            return;
        }

        Iterator<PersistentCometEvent> iterator = forced.iterator();
        while (iterator.hasNext()) {
            PersistentCometEvent comet = iterator.next();
            if (time > comet.startTime() + comet.durationTicks()) {
                iterator.remove();
            }
        }
    }

    private record NaturalCometCache(long dayIndex, long worldSeed, List<PersistentCometEvent> comets) {
    }
}
