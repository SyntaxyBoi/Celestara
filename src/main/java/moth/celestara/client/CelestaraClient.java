package moth.celestara.client;

import moth.celestara.CelestaraMod;
import moth.celestara.client.sky.CelestaraClientEventState;
import moth.celestara.client.sky.ForcedNightEvent;
import moth.celestara.client.sky.NightSkyPlanManager;
import moth.celestara.event.CelestaraNetworking;
import moth.celestara.event.PersistentCometSeed;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CelestaraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CelestaraMod.LOGGER.info("[Celestara] Initializing client-only night sky visuals...");
        ClientCommandRegistrationCallback.EVENT.register(CelestaraClientCommands::register);
        registerNetworking();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> NightSkyPlanManager.clear());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CelestaraClientEventState.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            NightSkyPlanManager.clear();
            CelestaraClientEventState.clear();
        });
    }

    private static void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(CelestaraNetworking.EVENT_SYNC, (client, handler, buffer, responseSender) -> {
            Identifier dimension = buffer.readIdentifier();
            int solarCount = buffer.readVarInt();
            Map<Long, Boolean> solarEclipses = new HashMap<>(solarCount * 2);
            for (int index = 0; index < solarCount; index++) {
                solarEclipses.put(buffer.readLong(), buffer.readBoolean());
            }
            int lunarCount = buffer.readVarInt();
            Map<Long, Boolean> lunarEclipses = new HashMap<>(lunarCount * 2);
            for (int index = 0; index < lunarCount; index++) {
                lunarEclipses.put(buffer.readLong(), buffer.readBoolean());
            }
            int cometCount = buffer.readVarInt();
            List<PersistentCometSeed> persistentComets = new ArrayList<>(cometCount);
            for (int index = 0; index < cometCount; index++) {
                persistentComets.add(new PersistentCometSeed(
                        buffer.readLong(),
                        buffer.readLong(),
                        buffer.readBoolean(),
                        buffer.readInt()
                ));
            }

            client.execute(() -> CelestaraClientEventState.applyServerSnapshot(
                    dimension,
                    solarEclipses,
                    lunarEclipses,
                    persistentComets
            ));
        });
        ClientPlayNetworking.registerGlobalReceiver(CelestaraNetworking.NIGHT_OVERRIDE, (client, handler, buffer, responseSender) -> {
            Identifier dimension = buffer.readIdentifier();
            long targetTime = buffer.readLong();
            ForcedNightEvent event = buffer.readEnumConstant(ForcedNightEvent.class);
            boolean enabled = buffer.readBoolean();
            int colorOverride = buffer.readInt();

            client.execute(() -> NightSkyPlanManager.applyCommandOverride(dimension, targetTime, event, enabled, colorOverride));
        });
    }
}
