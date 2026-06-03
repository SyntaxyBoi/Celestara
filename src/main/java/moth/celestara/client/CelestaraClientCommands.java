package moth.celestara.client;

import com.mojang.brigadier.CommandDispatcher;
import moth.celestara.client.sky.ForcedNightEvent;
import moth.celestara.client.sky.NightSkyPlanManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class CelestaraClientCommands {
    private static final int TIME_NIGHT = 13000;
    private static final int TIME_MIDNIGHT = 18000;

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
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_NIGHT, "night", ForcedNightEvent.COMET, true)))
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
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.COMET, true)))
                                                .then(literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.COMET, false))))
                                        .then(literal("meteor_shower")
                                                .then(literal("true")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.METEOR_SHOWER, true)))
                                                .then(literal("false")
                                                        .executes(context -> setNightEvent(context.getSource(), TIME_MIDNIGHT, "midnight", ForcedNightEvent.METEOR_SHOWER, false))))))));
    }

    private static int setNightEvent(FabricClientCommandSource source, int timeOfDay, String timeName,
                                     ForcedNightEvent event, boolean enabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
            source.sendFeedback(Text.literal("Celestara can only force night sky events while in a world."));
            return 0;
        }

        long currentDay = Math.floorDiv(client.world.getTimeOfDay(), 24000L);
        long targetTime = currentDay * 24000L + timeOfDay;
        Identifier dimension = client.world.getRegistryKey().getValue();
        NightSkyPlanManager.applyCommandOverride(dimension, targetTime, event, enabled);

        client.getNetworkHandler().sendChatCommand("time set " + timeName);
        source.sendFeedback(feedback(timeName, event, enabled));
        return (int) targetTime;
    }

    private static Text feedback(String timeName, ForcedNightEvent event, boolean enabled) {
        String eventName = event == ForcedNightEvent.METEOR_SHOWER ? "meteor shower" : "comet";
        if (event == ForcedNightEvent.COMET && enabled) {
            return Text.literal("Celestara queued a client-side early comet and asked the server to set time to " + timeName + ".");
        }
        if (event == ForcedNightEvent.METEOR_SHOWER && enabled) {
            return Text.literal("Celestara forced a client-side meteor shower and asked the server to set time to " + timeName + ".");
        }
        return Text.literal("Celestara cleared the forced client-side " + eventName + " override and asked the server to set time to " + timeName + ".");
    }
}
