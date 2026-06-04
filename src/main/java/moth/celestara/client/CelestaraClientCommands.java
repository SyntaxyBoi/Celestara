package moth.celestara.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import moth.celestara.event.CelestaraEvents;
import moth.celestara.client.sky.CometColorChoice;
import moth.celestara.client.sky.ForcedNightEvent;
import moth.celestara.client.sky.NightSkyPlan;
import moth.celestara.client.sky.NightSkyPlanManager;
import moth.celestara.client.sky.PersistentCometManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public final class CelestaraClientCommands {
    private static final int TIME_NIGHT = 13000;
    private static final int TIME_MIDNIGHT = 18000;
    private static final SuggestionProvider<FabricClientCommandSource> COMET_COLOR_SUGGESTIONS = (context, builder) -> {
        for (CometColorChoice choice : CometColorChoice.values()) {
            builder.suggest(choice.id());
        }
        return builder.buildFuture();
    };

    private CelestaraClientCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("celestara")
                .then(literal("time")
                        .then(literal("set")
                                .then(literal("night")
                                        .then(literal("comet")
                                                .then(literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, true))
                                                        .then(argument("color", StringArgumentType.word())
                                                                .suggests(COMET_COLOR_SUGGESTIONS)
                                                                .executes(context -> setNightEventWithColor(context.getSource(), TIME_NIGHT, "night",
                                                                        StringArgumentType.getString(context, "color")))))
                                                .then(literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, false))))
                                        .then(literal("meteor_shower")
                                                .then(literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, true)))
                                                .then(literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, false)))))
                                .then(literal("midnight")
                                        .then(literal("comet")
                                                .then(literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.COMET, true))
                                                        .then(argument("color", StringArgumentType.word())
                                                                .suggests(COMET_COLOR_SUGGESTIONS)
                                                                .executes(context -> setNightEventWithColor(context.getSource(), TIME_MIDNIGHT, "midnight",
                                                                        StringArgumentType.getString(context, "color")))))
                                                .then(literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.COMET, false))))
                                        .then(literal("meteor_shower")
                                                .then(literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.METEOR_SHOWER, true)))
                                                .then(literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.METEOR_SHOWER, false))))))));
        dispatcher.register(literal("celestara")
                .then(literal("eclipse")
                        .then(literal("solar")
                                .executes(context -> forceSolarEclipse(context.getSource()))
                                .then(argument("time", IntegerArgumentType.integer(0, 23999))
                                        .executes(context -> forceSolarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time"))))
                                .then(literal("time")
                                        .then(argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceSolarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))
                                .then(literal("true")
                                        .executes(context -> forceSolarEclipse(context.getSource()))
                                        .then(argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceSolarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time"))))))
                        .then(literal("lunar")
                                .executes(context -> forceLunarEclipse(context.getSource()))
                                .then(argument("time", IntegerArgumentType.integer(0, 23999))
                                        .executes(context -> forceLunarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time"))))
                                .then(literal("time")
                                        .then(argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceLunarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))
                                .then(literal("true")
                                        .executes(context -> forceLunarEclipse(context.getSource()))
                                        .then(argument("time", IntegerArgumentType.integer(0, 23999))
                                                .executes(context -> forceLunarEclipse(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))))
                .then(literal("persistent_comet")
                        .executes(context -> spawnPersistentComet(context.getSource()))
                        .then(argument("color", StringArgumentType.word())
                                .suggests(COMET_COLOR_SUGGESTIONS)
                                .executes(context -> spawnPersistentComet(context.getSource(), StringArgumentType.getString(context, "color")))))
                .then(literal("comet")
                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, true))
                        .then(argument("color", StringArgumentType.word())
                                .suggests(COMET_COLOR_SUGGESTIONS)
                                .executes(context -> setNightEventWithColor(context.getSource(), TIME_NIGHT, "night",
                                        StringArgumentType.getString(context, "color"))))
                        .then(literal("true")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, true))
                                .then(argument("color", StringArgumentType.word())
                                        .suggests(COMET_COLOR_SUGGESTIONS)
                                        .executes(context -> setNightEventWithColor(context.getSource(), TIME_NIGHT, "night",
                                                StringArgumentType.getString(context, "color")))))
                        .then(literal("false")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, false))))
                .then(literal("meteor_shower")
                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, true))
                        .then(literal("true")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, true)))
                        .then(literal("false")
                                .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.METEOR_SHOWER, false))))
                .then(literal("force")
                        .then(literal("quiet")
                                .executes(context -> queueNextNightMood(context.getSource(), ForcedNightEvent.QUIET_NIGHT, "quiet")))
                        .then(literal("loud")
                                .executes(context -> queueNextNightMood(context.getSource(), ForcedNightEvent.LOUD_NIGHT, "loud")))
                        .then(literal("normal")
                                .executes(context -> clearNextNightMood(context.getSource()))))
                .then(literal("refresh")
                        .executes(context -> refreshSky(context.getSource())))
                .then(literal("status")
                        .executes(context -> showStatus(context.getSource()))));
    }

    private static int setNightEvent(FabricClientCommandSource source, int timeOfDay, String timeName,
                                     ForcedNightEvent event, boolean enabled) {
        return setNightEvent(source, timeOfDay, timeName, event, enabled, CometColorChoice.RANDOM_COLOR);
    }

    private static int setNightEventWithColor(FabricClientCommandSource source, int timeOfDay,
                                              String timeName, String colorName) {
        CometColorChoice color = parseCometColor(source, colorName);
        if (color == null) {
            return 0;
        }
        return setNightEvent(source, timeOfDay, timeName, ForcedNightEvent.COMET, true, color.color());
    }

    private static int setNightEvent(FabricClientCommandSource source, int timeOfDay, String timeName,
                                     ForcedNightEvent event, boolean enabled, int colorOverride) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara can only force night sky events while in a world."));
            return 0;
        }
        if (!NightSkyPlanManager.isAllowedDimension(client.world)) {
            source.sendFeedback(Text.literal("Celestara night sky events can only render in the Overworld."));
            return 0;
        }

        long currentDay = CelestaraEvents.dayIndex(client.world.getTimeOfDay());
        long targetTime = currentDay * CelestaraEvents.DAY_LENGTH_TICKS + timeOfDay;
        Identifier dimension = client.world.getRegistryKey().getValue();
        NightSkyPlanManager.applyCommandOverride(dimension, targetTime, event, enabled, colorOverride);

        setTime(client, targetTime, timeName);
        source.sendFeedback(feedback(timeName, event, enabled, colorOverride));
        return (int) targetTime;
    }

    private static int queueNextNightMood(FabricClientCommandSource source, ForcedNightEvent event, String moodName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara can only force night sky moods while in a world."));
            return 0;
        }
        if (!NightSkyPlanManager.isAllowedDimension(client.world)) {
            source.sendFeedback(Text.literal("Celestara night sky moods can only render in the Overworld."));
            return 0;
        }

        long targetTime = nextNightStart(client.world.getTimeOfDay());
        Identifier dimension = client.world.getRegistryKey().getValue();
        NightSkyPlanManager.applyCommandOverride(dimension, targetTime, event, true);
        source.sendFeedback(Text.literal("Celestara queued the current/next client-side night as " + moodName
                + ". Run /celestara refresh if that night sky is already loaded."));
        return 1;
    }

    private static int clearNextNightMood(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara can only clear night sky moods while in a world."));
            return 0;
        }
        if (!NightSkyPlanManager.isAllowedDimension(client.world)) {
            source.sendFeedback(Text.literal("Celestara night sky moods can only render in the Overworld."));
            return 0;
        }

        long targetTime = nextNightStart(client.world.getTimeOfDay());
        Identifier dimension = client.world.getRegistryKey().getValue();
        NightSkyPlanManager.applyCommandOverride(dimension, targetTime, ForcedNightEvent.CLEAR_NIGHT_MOOD, true);
        source.sendFeedback(Text.literal("Celestara cleared the forced mood for the current/next client-side night."));
        return 1;
    }

    private static int refreshSky(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara can only refresh the sky while in a world."));
            return 0;
        }
        if (!NightSkyPlanManager.isAllowedDimension(client.world)) {
            source.sendFeedback(Text.literal("Celestara sky plans only render in the Overworld."));
            return 0;
        }

        Identifier dimension = client.world.getRegistryKey().getValue();
        boolean refreshed = NightSkyPlanManager.refresh(dimension, client.world.getTimeOfDay());
        source.sendFeedback(Text.literal(refreshed
                ? "Celestara refreshed the current client-side sky plan."
                : "Celestara has no current client-side sky plan to refresh yet."));
        return 1;
    }

    private static int showStatus(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara status is only available while in a world."));
            return 0;
        }
        if (!NightSkyPlanManager.isAllowedDimension(client.world)) {
            source.sendFeedback(Text.literal("Celestara sky plans only render in the Overworld."));
            return 0;
        }

        NightSkyPlan plan = NightSkyPlanManager.currentPlan();
        int phase = CelestaraEvents.dayPhase(client.world.getTimeOfDay());
        if (phase < CelestaraEvents.NIGHT_START_TICK) {
            source.sendFeedback(Text.literal("Celestara has no active night sky plan right now. Current time is "
                    + phase + "; night begins at " + CelestaraEvents.NIGHT_START_TICK + "."));
            return 1;
        }
        if (plan == null) {
            source.sendFeedback(Text.literal("Celestara has no loaded sky plan yet. Current time is " + phase
                    + "; night begins at " + CelestaraEvents.NIGHT_START_TICK + "."));
            return 1;
        }

        long supergiants = plan.stars().stream().filter(star -> star.supergiant()).count();
        source.sendFeedback(Text.literal("Celestara sky: " + plan.mood().name().toLowerCase()
                + (plan.forcedMood() ? " forced" : "")
                + ", " + plan.stars().size() + " stars"
                + ", " + supergiants + " supergiants"
                + ", " + plan.shootingStars().size() + " shooting stars"
                + ", " + plan.comets().size() + " comets."));
        return 1;
    }

    private static int forceSolarEclipse(FabricClientCommandSource source) {
        return forceSolarEclipse(source, CelestaraEvents.SOLAR_COMMAND_TIME);
    }

    private static int forceSolarEclipse(FabricClientCommandSource source, int setTime) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara can only force eclipses while in a world."));
            return 0;
        }
        if (!CelestaraEvents.isAllowedWorld(client.world)) {
            source.sendFeedback(Text.literal("Celestara eclipses can only be forced in the Overworld."));
            return 0;
        }

        long currentDay = CelestaraEvents.dayIndex(client.world.getTimeOfDay());
        long eclipseDay = currentDay + 1L;
        long targetDay = setTime <= CelestaraEvents.SOLAR_VISIBLE_END_TICK ? eclipseDay : currentDay;
        Identifier dimension = client.world.getRegistryKey().getValue();
        CelestaraEvents.forceSolarEclipse(dimension, eclipseDay);
        long targetTime = targetDay * CelestaraEvents.DAY_LENGTH_TICKS + setTime;
        setTime(client, targetTime, Integer.toString(setTime));
        source.sendFeedback(Text.literal("Celestara forced a client-side solar eclipse and set time to " + setTime + "."));
        return 1;
    }

    private static int forceLunarEclipse(FabricClientCommandSource source) {
        return forceLunarEclipse(source, CelestaraEvents.LUNAR_COMMAND_TIME);
    }

    private static int forceLunarEclipse(FabricClientCommandSource source, int setTime) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara can only force eclipses while in a world."));
            return 0;
        }
        if (!CelestaraEvents.isAllowedWorld(client.world)) {
            source.sendFeedback(Text.literal("Celestara eclipses can only be forced in the Overworld."));
            return 0;
        }

        long currentTime = client.world.getTimeOfDay();
        long currentDay = CelestaraEvents.dayIndex(currentTime);
        long eclipseNight = CelestaraEvents.isNight(currentTime) ? currentDay + 1L : currentDay;
        long targetDay = setTime <= CelestaraEvents.LUNAR_VISIBLE_END_TICK ? eclipseNight + 1L : eclipseNight;
        Identifier dimension = client.world.getRegistryKey().getValue();
        CelestaraEvents.forceLunarEclipse(dimension, eclipseNight);
        long targetTime = targetDay * CelestaraEvents.DAY_LENGTH_TICKS + setTime;
        setTime(client, targetTime, Integer.toString(setTime));
        source.sendFeedback(Text.literal("Celestara forced a client-side lunar eclipse and set time to " + setTime + "."));
        return 1;
    }

    private static int spawnPersistentComet(FabricClientCommandSource source) {
        return spawnPersistentComet(source, CometColorChoice.RANDOM.id());
    }

    private static int spawnPersistentComet(FabricClientCommandSource source, String colorName) {
        CometColorChoice color = parseCometColor(source, colorName);
        if (color == null) {
            return 0;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (!hasUsableWorld(client)) {
            source.sendFeedback(Text.literal("Celestara can only spawn a persistent comet while in a world."));
            return 0;
        }
        if (!NightSkyPlanManager.isAllowedDimension(client.world)) {
            source.sendFeedback(Text.literal("Celestara persistent comets can only render in the Overworld."));
            return 0;
        }

        PersistentCometManager.spawnCommandComet(client.world, color.color());
        source.sendFeedback(Text.literal("Celestara spawned a client-side persistent comet"
                + colorDescription(color.color()) + "."));
        return 1;
    }

    private static void setTime(MinecraftClient client, long targetTime, String fallbackTimeArgument) {
        if (client.getServer() != null) {
            for (ServerWorld world : client.getServer().getWorlds()) {
                world.setTimeOfDay(targetTime);
            }
            if (client.world != null) {
                client.world.setTimeOfDay(targetTime);
            }
        } else if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand("time set " + fallbackTimeArgument);
        }
    }

    private static long nextNightStart(long timeOfDay) {
        long currentDay = CelestaraEvents.dayIndex(timeOfDay);
        return currentDay * CelestaraEvents.DAY_LENGTH_TICKS + CelestaraEvents.NIGHT_START_TICK;
    }

    private static boolean hasUsableWorld(MinecraftClient client) {
        return client.world != null && client.player != null;
    }

    private static Text feedback(String timeName, ForcedNightEvent event, boolean enabled, int colorOverride) {
        String eventName = event == ForcedNightEvent.METEOR_SHOWER ? "meteor shower" : "comet";
        if (event == ForcedNightEvent.COMET && enabled) {
            return Text.literal("Celestara queued a client-side early comet" + colorDescription(colorOverride)
                    + " and asked the server to set time to " + timeName + ".");
        }
        if (event == ForcedNightEvent.METEOR_SHOWER && enabled) {
            return Text.literal("Celestara forced a client-side meteor shower and asked the server to set time to " + timeName + ".");
        }
        return Text.literal("Celestara cleared the forced client-side " + eventName + " override and asked the server to set time to " + timeName + ".");
    }

    private static CometColorChoice parseCometColor(FabricClientCommandSource source, String colorName) {
        CometColorChoice color = CometColorChoice.byName(colorName);
        if (color == null) {
            source.sendFeedback(Text.literal("Unknown comet color '" + colorName + "'. Try: " + CometColorChoice.names() + "."));
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
}
