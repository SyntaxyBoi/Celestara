package moth.celestara.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import moth.celestara.client.sky.CometColorChoice;
import moth.celestara.client.sky.ForcedNightEvent;
import moth.celestara.client.sky.ForcedNightSkyOptions;
import moth.celestara.client.sky.NightSkyMood;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class CelestaraServerEvents {
    private static final Map<Identifier, List<PersistentCometSeed>> FORCED_PERSISTENT_COMETS = new HashMap<>();
    private static final Map<Identifier, Map<Long, ForcedNightSkyOptions>> FORCED_NIGHT_OPTIONS = new HashMap<>();
    private static final int TIME_NIGHT = 13000;
    private static final int TIME_MIDNIGHT = 18000;
    private static final SuggestionProvider<ServerCommandSource> COMET_COLOR_SUGGESTIONS = (context, builder) -> {
        for (CometColorChoice choice : CometColorChoice.values()) {
            builder.suggest(choice.id());
        }
        return builder.buildFuture();
    };

    private CelestaraServerEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(CelestaraServerEvents::registerCommands);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncPlayer(handler.player));
        ServerTickEvents.END_WORLD_TICK.register(CelestaraServerEvents::syncWorld);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            FORCED_PERSISTENT_COMETS.clear();
            FORCED_NIGHT_OPTIONS.clear();
            CelestaraEvents.clearForcedEclipses();
        });
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("celestara")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("time")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("night")
                                        .then(CommandManager.literal("comet")
                                                .then(CommandManager.literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, true))
                                                        .then(CommandManager.argument("color", StringArgumentType.word())
                                                                .suggests(COMET_COLOR_SUGGESTIONS)
                                                                .executes(context -> setNightEventWithColor(context.getSource(), TIME_NIGHT, "night",
                                                                        StringArgumentType.getString(context, "color")))))
                                                .then(CommandManager.literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, false))))
                                        .then(CommandManager.literal("meteor_shower")
                                                .then(CommandManager.literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, true)))
                                                .then(CommandManager.literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, false)))))
                                .then(CommandManager.literal("midnight")
                                        .then(CommandManager.literal("comet")
                                                .then(CommandManager.literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.COMET, true))
                                                        .then(CommandManager.argument("color", StringArgumentType.word())
                                                                .suggests(COMET_COLOR_SUGGESTIONS)
                                                                .executes(context -> setNightEventWithColor(context.getSource(), TIME_MIDNIGHT, "midnight",
                                                                        StringArgumentType.getString(context, "color")))))
                                                .then(CommandManager.literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.COMET, false))))
                                        .then(CommandManager.literal("meteor_shower")
                                                .then(CommandManager.literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.METEOR_SHOWER, true)))
                                                .then(CommandManager.literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.METEOR_SHOWER, false)))))))
                .then(CommandManager.literal("eclipse")
                        .then(CommandManager.literal("solar")
                                .executes(context -> forceSolarEclipse(context.getSource()))
                                .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 23999))
                                        .executes(context -> forceSolarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time"))))
                                .then(CommandManager.literal("time")
                                        .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceSolarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))
                                .then(CommandManager.literal("true")
                                        .executes(context -> forceSolarEclipse(context.getSource()))
                                        .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceSolarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time"))))))
                        .then(CommandManager.literal("lunar")
                                .executes(context -> forceLunarEclipse(context.getSource()))
                                .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 23999))
                                        .executes(context -> forceLunarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time"))))
                                .then(CommandManager.literal("time")
                                        .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceLunarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))
                                .then(CommandManager.literal("true")
                                        .executes(context -> forceLunarEclipse(context.getSource()))
                                        .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceLunarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))))
                .then(CommandManager.literal("persistent_comet")
                        .executes(context -> spawnPersistentComet(context.getSource()))
                        .then(CommandManager.argument("color", StringArgumentType.word())
                                .suggests(COMET_COLOR_SUGGESTIONS)
                                .executes(context -> spawnPersistentComet(context.getSource(), StringArgumentType.getString(context, "color")))))
                .then(CommandManager.literal("comet")
                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, true))
                        .then(CommandManager.argument("color", StringArgumentType.word())
                                .suggests(COMET_COLOR_SUGGESTIONS)
                                .executes(context -> setNightEventWithColor(context.getSource(), TIME_NIGHT, "night",
                                        StringArgumentType.getString(context, "color"))))
                        .then(CommandManager.literal("true")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, true))
                                .then(CommandManager.argument("color", StringArgumentType.word())
                                        .suggests(COMET_COLOR_SUGGESTIONS)
                                        .executes(context -> setNightEventWithColor(context.getSource(), TIME_NIGHT, "night",
                                                StringArgumentType.getString(context, "color")))))
                        .then(CommandManager.literal("false")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, false))))
                .then(CommandManager.literal("meteor_shower")
                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, true))
                        .then(CommandManager.literal("true")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, true)))
                        .then(CommandManager.literal("false")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, false))))
                .then(CommandManager.literal("force")
                        .then(CommandManager.literal("quiet")
                                .executes(context -> queueNextNightMood(context.getSource(), ForcedNightEvent.QUIET_NIGHT, "quiet")))
                        .then(CommandManager.literal("loud")
                                .executes(context -> queueNextNightMood(context.getSource(), ForcedNightEvent.LOUD_NIGHT, "loud")))
                        .then(CommandManager.literal("normal")
                                .executes(context -> clearNextNightMood(context.getSource()))))
                .then(CommandManager.literal("refresh")
                        .executes(context -> refreshSky(context.getSource())))
                .then(CommandManager.literal("status")
                        .executes(context -> showStatus(context.getSource()))));
    }

    private static int setNightEvent(ServerCommandSource source, int timeOfDay, String timeName,
                                     ForcedNightEvent event, boolean enabled) {
        return setNightEvent(source, timeOfDay, timeName, event, enabled, CometColorChoice.RANDOM_COLOR);
    }

    private static int setNightEventWithColor(ServerCommandSource source, int timeOfDay,
                                              String timeName, String colorName) {
        CometColorChoice color = parseCometColor(source, colorName);
        if (color == null) {
            return 0;
        }
        return setNightEvent(source, timeOfDay, timeName, ForcedNightEvent.COMET, true, color.color());
    }

    private static int setNightEvent(ServerCommandSource source, int timeOfDay, String timeName,
                                     ForcedNightEvent event, boolean enabled, int colorOverride) {
        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara night sky events can only be forced in the Overworld."));
            return 0;
        }

        long currentDay = CelestaraEvents.dayIndex(world.getTimeOfDay());
        long targetTime = currentDay * CelestaraEvents.DAY_LENGTH_TICKS + timeOfDay;
        setTimeForAllWorlds(source.getServer(), targetTime);
        queueNightOverride(source.getServer(), world.getRegistryKey().getValue(), targetTime, event, enabled, colorOverride);
        source.sendFeedback(() -> feedback(timeName, event, enabled, colorOverride), true);
        return 1;
    }

    private static int queueNextNightMood(ServerCommandSource source, ForcedNightEvent event, String moodName) {
        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara night sky moods can only be forced in the Overworld."));
            return 0;
        }

        long targetTime = nextNightStart(world.getTimeOfDay());
        queueNightOverride(source.getServer(), world.getRegistryKey().getValue(), targetTime, event, true, CometColorChoice.RANDOM_COLOR);
        source.sendFeedback(() -> Text.literal("Celestara queued the current/next night as " + moodName
                + ". Run /celestara refresh if that night sky is already loaded."), true);
        return 1;
    }

    private static int clearNextNightMood(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara night sky moods can only be cleared in the Overworld."));
            return 0;
        }

        long targetTime = nextNightStart(world.getTimeOfDay());
        queueNightOverride(source.getServer(), world.getRegistryKey().getValue(), targetTime, ForcedNightEvent.CLEAR_NIGHT_MOOD, true, CometColorChoice.RANDOM_COLOR);
        source.sendFeedback(() -> Text.literal("Celestara cleared the forced mood for the current/next night."), true);
        return 1;
    }

    private static int refreshSky(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara can only refresh sky plans in the Overworld."));
            return 0;
        }

        replayStoredNightOverrides(source.getServer(), world.getRegistryKey().getValue(), world.getTimeOfDay());
        sendNightOverride(source.getServer(), world.getRegistryKey().getValue(), world.getTimeOfDay(),
                ForcedNightEvent.REFRESH_SKY, true, CometColorChoice.RANDOM_COLOR);
        source.sendFeedback(() -> Text.literal("Celestara refreshed the current sky plan for connected clients."), true);
        return 1;
    }

    private static int showStatus(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara sky status is only available in the Overworld."));
            return 0;
        }

        Identifier dimension = world.getRegistryKey().getValue();
        long time = world.getTimeOfDay();
        long currentNight = CelestaraEvents.dayIndex(time);
        long queuedNight = CelestaraEvents.dayIndex(nextNightStart(time));
        String currentLabel = CelestaraEvents.isNight(time) ? "active night" : "tonight";
        String currentStatus = describeStoredOptions(dimension, currentNight);
        String queuedStatus = queuedNight == currentNight
                ? currentStatus
                : describeStoredOptions(dimension, queuedNight);

        source.sendFeedback(() -> Text.literal("Celestara server sky overrides: "
                + currentLabel + " " + currentNight + " = " + currentStatus
                + "; next command target " + queuedNight + " = " + queuedStatus + "."), false);
        return 1;
    }

    private static int forceSolarEclipse(ServerCommandSource source) {
        return forceSolarEclipse(source, CelestaraEvents.SOLAR_COMMAND_TIME);
    }

    private static int forceSolarEclipse(ServerCommandSource source, int setTime) {
        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara eclipses can only be forced in the Overworld."));
            return 0;
        }

        long currentDay = CelestaraEvents.dayIndex(world.getTimeOfDay());
        long eclipseDay = currentDay + 1L;
        long targetDay = setTime <= CelestaraEvents.SOLAR_VISIBLE_END_TICK ? eclipseDay : currentDay;
        long targetTime = targetDay * CelestaraEvents.DAY_LENGTH_TICKS + setTime;
        CelestaraEvents.forceSolarEclipse(world.getRegistryKey().getValue(), eclipseDay);
        setTimeForAllWorlds(source.getServer(), targetTime);
        syncAllPlayers(source.getServer());
        source.sendFeedback(() -> Text.literal("Celestara forced a solar eclipse and set time to " + setTime + "."), true);
        return 1;
    }

    private static int forceLunarEclipse(ServerCommandSource source) {
        return forceLunarEclipse(source, CelestaraEvents.LUNAR_COMMAND_TIME);
    }

    private static int forceLunarEclipse(ServerCommandSource source, int setTime) {
        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara eclipses can only be forced in the Overworld."));
            return 0;
        }

        long currentTime = world.getTimeOfDay();
        long currentDay = CelestaraEvents.dayIndex(currentTime);
        long eclipseNight = CelestaraEvents.isNight(currentTime) ? currentDay + 1L : currentDay;
        long targetDay = setTime <= CelestaraEvents.LUNAR_VISIBLE_END_TICK ? eclipseNight + 1L : eclipseNight;
        long targetTime = targetDay * CelestaraEvents.DAY_LENGTH_TICKS + setTime;
        CelestaraEvents.forceLunarEclipse(world.getRegistryKey().getValue(), eclipseNight);
        setTimeForAllWorlds(source.getServer(), targetTime);
        syncAllPlayers(source.getServer());
        source.sendFeedback(() -> Text.literal("Celestara forced a lunar eclipse and set time to " + setTime + "."), true);
        return 1;
    }

    private static int spawnPersistentComet(ServerCommandSource source) {
        return spawnPersistentComet(source, CometColorChoice.RANDOM.id());
    }

    private static int spawnPersistentComet(ServerCommandSource source, String colorName) {
        CometColorChoice color = parseCometColor(source, colorName);
        if (color == null) {
            return 0;
        }

        ServerWorld world = source.getWorld();
        if (!CelestaraEvents.isAllowedWorld(world)) {
            source.sendError(Text.literal("Celestara persistent comets can only be spawned in the Overworld."));
            return 0;
        }

        Identifier dimension = world.getRegistryKey().getValue();
        long startTime = world.getTimeOfDay();
        long seed = CelestaraEvents.stableSeed(
                world.getSeed(),
                dimension,
                startTime ^ world.getTime(),
                CelestaraEvents.PERSISTENT_COMET_SHAPE_SALT
        );
        FORCED_PERSISTENT_COMETS
                .computeIfAbsent(dimension, ignored -> new ArrayList<>())
                .add(new PersistentCometSeed(startTime, seed, true, color.color()));
        syncWorldNow(world);
        source.sendFeedback(() -> Text.literal("Celestara spawned a persistent comet" + colorDescription(color.color()) + "."), true);
        return 1;
    }

    private static void syncWorld(ServerWorld world) {
        if (!CelestaraEvents.isAllowedWorld(world) || world.getTime() % 20L != 0L) {
            return;
        }

        pruneForcedComets(world);
        pruneForcedNightOptions(world);
        syncWorldNow(world);
    }

    private static void syncWorldNow(ServerWorld world) {
        EventSnapshot snapshot = createSnapshot(world);
        if (snapshot == null) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            sendSnapshot(player, snapshot);
        }
    }

    private static void syncPlayer(ServerPlayerEntity player) {
        EventSnapshot snapshot = createSnapshot(player.getServerWorld());
        if (snapshot != null) {
            sendSnapshot(player, snapshot);
            replayStoredNightOverrides(player, snapshot.dimension(), player.getServerWorld().getTimeOfDay());
        }
    }

    private static void syncAllPlayers(MinecraftServer server) {
        Map<ServerWorld, EventSnapshot> snapshots = new HashMap<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            EventSnapshot snapshot = snapshots.computeIfAbsent(player.getServerWorld(), CelestaraServerEvents::createSnapshot);
            if (snapshot != null) {
                sendSnapshot(player, snapshot);
            }
        }
    }

    private static void sendNightOverride(MinecraftServer server, Identifier dimension, long targetTime,
                                          ForcedNightEvent event, boolean enabled) {
        sendNightOverride(server, dimension, targetTime, event, enabled, CometColorChoice.RANDOM_COLOR);
    }

    private static void sendNightOverride(MinecraftServer server, Identifier dimension, long targetTime,
                                          ForcedNightEvent event, boolean enabled, int colorOverride) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendNightOverride(player, dimension, targetTime, event, enabled, colorOverride);
        }
    }

    private static void sendNightOverride(ServerPlayerEntity player, Identifier dimension, long targetTime,
                                          ForcedNightEvent event, boolean enabled) {
        sendNightOverride(player, dimension, targetTime, event, enabled, CometColorChoice.RANDOM_COLOR);
    }

    private static void sendNightOverride(ServerPlayerEntity player, Identifier dimension, long targetTime,
                                          ForcedNightEvent event, boolean enabled, int colorOverride) {
        if (!ServerPlayNetworking.canSend(player, CelestaraNetworking.NIGHT_OVERRIDE)) {
            return;
        }

        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeIdentifier(dimension);
        buffer.writeLong(targetTime);
        buffer.writeEnumConstant(event);
        buffer.writeBoolean(enabled);
        buffer.writeInt(colorOverride);
        ServerPlayNetworking.send(player, CelestaraNetworking.NIGHT_OVERRIDE, buffer);
    }

    private static void queueNightOverride(MinecraftServer server, Identifier dimension, long targetTime,
                                           ForcedNightEvent event, boolean enabled, int colorOverride) {
        storeNightOverride(dimension, targetTime, event, enabled, colorOverride);
        sendNightOverride(server, dimension, targetTime, event, enabled, colorOverride);
    }

    private static void storeNightOverride(Identifier dimension, long targetTime, ForcedNightEvent event,
                                           boolean enabled, int colorOverride) {
        if (event == ForcedNightEvent.REFRESH_SKY) {
            return;
        }

        long nightIndex = CelestaraEvents.dayIndex(targetTime);
        Map<Long, ForcedNightSkyOptions> optionsByNight = FORCED_NIGHT_OPTIONS.computeIfAbsent(dimension, ignored -> new HashMap<>());
        ForcedNightSkyOptions existing = optionsByNight.getOrDefault(nightIndex, ForcedNightSkyOptions.NONE);
        ForcedNightSkyOptions updated;
        int offset = nightOffsetForTime(targetTime);

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
            optionsByNight.remove(nightIndex);
            if (optionsByNight.isEmpty()) {
                FORCED_NIGHT_OPTIONS.remove(dimension);
            }
        } else {
            optionsByNight.put(nightIndex, updated);
        }
    }

    private static void replayStoredNightOverrides(MinecraftServer server, Identifier dimension, long timeOfDay) {
        Map<Long, ForcedNightSkyOptions> optionsByNight = FORCED_NIGHT_OPTIONS.get(dimension);
        if (optionsByNight == null || optionsByNight.isEmpty()) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            replayStoredNightOverrides(player, dimension, timeOfDay);
        }
    }

    private static void replayStoredNightOverrides(ServerPlayerEntity player, Identifier dimension, long timeOfDay) {
        Map<Long, ForcedNightSkyOptions> optionsByNight = FORCED_NIGHT_OPTIONS.get(dimension);
        if (optionsByNight == null || optionsByNight.isEmpty()) {
            return;
        }

        long currentNight = CelestaraEvents.dayIndex(timeOfDay);
        for (Map.Entry<Long, ForcedNightSkyOptions> entry : optionsByNight.entrySet()) {
            long nightIndex = entry.getKey();
            if (nightIndex < currentNight) {
                continue;
            }

            replayStoredNightOverride(player, dimension, nightIndex, entry.getValue());
        }
    }

    private static void replayStoredNightOverride(ServerPlayerEntity player, Identifier dimension, long nightIndex,
                                                  ForcedNightSkyOptions options) {
        long nightStart = nightIndex * CelestaraEvents.DAY_LENGTH_TICKS + CelestaraEvents.NIGHT_START_TICK;
        if (options.forcedMood() == NightSkyMood.QUIET) {
            sendNightOverride(player, dimension, nightStart, ForcedNightEvent.QUIET_NIGHT, true);
        } else if (options.forcedMood() == NightSkyMood.LOUD) {
            sendNightOverride(player, dimension, nightStart, ForcedNightEvent.LOUD_NIGHT, true);
        }

        if (options.forceMeteorShower()) {
            sendNightOverride(player, dimension, nightStart, ForcedNightEvent.METEOR_SHOWER, true);
        } else if (options.forceCommandComet()) {
            long targetTime = nightStart + options.commandStartOffset();
            sendNightOverride(player, dimension, targetTime, ForcedNightEvent.COMET, true, options.commandCometColor());
        }
    }

    private static EventSnapshot createSnapshot(ServerWorld world) {
        if (!CelestaraEvents.isAllowedWorld(world)) {
            return null;
        }

        Identifier dimension = world.getRegistryKey().getValue();
        long dayIndex = CelestaraEvents.dayIndex(world.getTimeOfDay());
        long firstSolarDay = dayIndex - 1L;
        long firstLunarNight = dayIndex - 1L;
        boolean[] solarEclipses = new boolean[3];
        boolean[] lunarEclipses = new boolean[3];

        for (int index = 0; index < solarEclipses.length; index++) {
            solarEclipses[index] = CelestaraEvents.isSolarEclipse(world, firstSolarDay + index);
        }
        for (int index = 0; index < lunarEclipses.length; index++) {
            lunarEclipses[index] = CelestaraEvents.isLunarEclipse(world, firstLunarNight + index);
        }

        return new EventSnapshot(
                dimension,
                firstSolarDay,
                solarEclipses,
                firstLunarNight,
                lunarEclipses,
                activePersistentComets(world)
        );
    }

    private static void sendSnapshot(ServerPlayerEntity player, EventSnapshot snapshot) {
        if (!ServerPlayNetworking.canSend(player, CelestaraNetworking.EVENT_SYNC)) {
            return;
        }

        PacketByteBuf buffer = PacketByteBufs.create();

        buffer.writeIdentifier(snapshot.dimension());
        buffer.writeVarInt(snapshot.solarEclipses().length);
        for (int index = 0; index < snapshot.solarEclipses().length; index++) {
            long solarDay = snapshot.firstSolarDay() + index;
            buffer.writeLong(solarDay);
            buffer.writeBoolean(snapshot.solarEclipses()[index]);
        }
        buffer.writeVarInt(snapshot.lunarEclipses().length);
        for (int index = 0; index < snapshot.lunarEclipses().length; index++) {
            long lunarNight = snapshot.firstLunarNight() + index;
            buffer.writeLong(lunarNight);
            buffer.writeBoolean(snapshot.lunarEclipses()[index]);
        }

        buffer.writeVarInt(snapshot.comets().size());
        for (PersistentCometSeed comet : snapshot.comets()) {
            buffer.writeLong(comet.startTime());
            buffer.writeLong(comet.seed());
            buffer.writeBoolean(comet.commandForced());
            buffer.writeInt(comet.colorOverride());
        }

        ServerPlayNetworking.send(player, CelestaraNetworking.EVENT_SYNC, buffer);
    }

    private static List<PersistentCometSeed> activePersistentComets(ServerWorld world) {
        List<PersistentCometSeed> comets = new ArrayList<>();
        Identifier dimension = world.getRegistryKey().getValue();
        long time = world.getTimeOfDay();
        long currentDay = CelestaraEvents.dayIndex(time);

        for (long nightIndex = currentDay - 5L; nightIndex <= currentDay; nightIndex++) {
            long startTime = nightIndex * CelestaraEvents.DAY_LENGTH_TICKS + CelestaraEvents.NIGHT_START_TICK;
            long endTime = startTime + CelestaraEvents.PERSISTENT_COMET_DURATION_TICKS;
            if (time < startTime || time > endTime) {
                continue;
            }

            long rollSeed = CelestaraEvents.stableSeed(
                    world.getSeed(),
                    dimension,
                    nightIndex,
                    CelestaraEvents.PERSISTENT_COMET_ROLL_SALT
            );
            if (CelestaraEvents.stableUnit(rollSeed) < CelestaraEvents.PERSISTENT_COMET_CHANCE) {
                long shapeSeed = CelestaraEvents.stableSeed(
                        world.getSeed(),
                        dimension,
                        nightIndex,
                        CelestaraEvents.PERSISTENT_COMET_SHAPE_SALT
                );
                comets.add(new PersistentCometSeed(startTime, shapeSeed, false, CometColorChoice.RANDOM_COLOR));
            }
        }

        List<PersistentCometSeed> forced = FORCED_PERSISTENT_COMETS.get(dimension);
        if (forced != null) {
            comets.addAll(forced);
        }

        return comets;
    }

    private static void pruneForcedComets(ServerWorld world) {
        Identifier dimension = world.getRegistryKey().getValue();
        List<PersistentCometSeed> forced = FORCED_PERSISTENT_COMETS.get(dimension);
        if (forced == null) {
            return;
        }

        long time = world.getTimeOfDay();
        Iterator<PersistentCometSeed> iterator = forced.iterator();
        while (iterator.hasNext()) {
            PersistentCometSeed comet = iterator.next();
            if (time > comet.startTime() + CelestaraEvents.PERSISTENT_COMET_DURATION_TICKS) {
                iterator.remove();
            }
        }
    }

    private static void pruneForcedNightOptions(ServerWorld world) {
        Identifier dimension = world.getRegistryKey().getValue();
        Map<Long, ForcedNightSkyOptions> optionsByNight = FORCED_NIGHT_OPTIONS.get(dimension);
        if (optionsByNight == null) {
            return;
        }

        long earliestRelevantNight = CelestaraEvents.dayIndex(world.getTimeOfDay());
        Iterator<Long> iterator = optionsByNight.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next() < earliestRelevantNight) {
                iterator.remove();
            }
        }
        if (optionsByNight.isEmpty()) {
            FORCED_NIGHT_OPTIONS.remove(dimension);
        }
    }

    private static void setTimeForAllWorlds(MinecraftServer server, long timeOfDay) {
        for (ServerWorld world : server.getWorlds()) {
            world.setTimeOfDay(timeOfDay);
        }
    }

    private static long nextNightStart(long timeOfDay) {
        long currentDay = CelestaraEvents.dayIndex(timeOfDay);
        return currentDay * CelestaraEvents.DAY_LENGTH_TICKS + CelestaraEvents.NIGHT_START_TICK;
    }

    private static int nightOffsetForTime(long timeOfDay) {
        int phase = CelestaraEvents.dayPhase(timeOfDay);
        if (phase < CelestaraEvents.NIGHT_START_TICK) {
            return 0;
        }
        return Math.min(
                phase - CelestaraEvents.NIGHT_START_TICK,
                (int) (CelestaraEvents.DAY_LENGTH_TICKS - CelestaraEvents.NIGHT_START_TICK)
        );
    }

    private static Text feedback(String timeName, ForcedNightEvent event, boolean enabled, int colorOverride) {
        String eventName = event == ForcedNightEvent.METEOR_SHOWER ? "meteor shower" : "comet";
        if (event == ForcedNightEvent.COMET && enabled) {
            return Text.literal("Celestara queued an early comet" + colorDescription(colorOverride)
                    + " and set time to " + timeName + ".");
        }
        if (event == ForcedNightEvent.METEOR_SHOWER && enabled) {
            return Text.literal("Celestara forced a meteor shower and set time to " + timeName + ".");
        }
        return Text.literal("Celestara cleared the forced " + eventName + " override and set time to " + timeName + ".");
    }

    private static CometColorChoice parseCometColor(ServerCommandSource source, String colorName) {
        CometColorChoice color = CometColorChoice.byName(colorName);
        if (color == null) {
            source.sendError(Text.literal("Unknown comet color '" + colorName + "'. Try: " + CometColorChoice.names() + "."));
        }
        return color;
    }

    private static String colorDescription(int colorOverride) {
        if (colorOverride == CometColorChoice.RANDOM_COLOR) {
            return "";
        }
        for (CometColorChoice choice : CometColorChoice.values()) {
            if (choice.color() == colorOverride) {
                return " (" + choice.id() + ")";
            }
        }
        return "";
    }

    private static String describeStoredOptions(Identifier dimension, long nightIndex) {
        Map<Long, ForcedNightSkyOptions> optionsByNight = FORCED_NIGHT_OPTIONS.get(dimension);
        ForcedNightSkyOptions options = optionsByNight == null
                ? ForcedNightSkyOptions.NONE
                : optionsByNight.getOrDefault(nightIndex, ForcedNightSkyOptions.NONE);
        if (options.isEmpty()) {
            return "natural";
        }

        List<String> parts = new ArrayList<>(3);
        if (options.forcedMood() == NightSkyMood.QUIET) {
            parts.add("quiet");
        } else if (options.forcedMood() == NightSkyMood.LOUD) {
            parts.add("loud");
        }
        if (options.forceMeteorShower()) {
            parts.add("meteor shower");
        } else if (options.forceCommandComet()) {
            parts.add("comet" + colorDescription(options.commandCometColor()));
        }
        return String.join(", ", parts);
    }

    private record EventSnapshot(
            Identifier dimension,
            long firstSolarDay,
            boolean[] solarEclipses,
            long firstLunarNight,
            boolean[] lunarEclipses,
            List<PersistentCometSeed> comets
    ) {
    }
}
