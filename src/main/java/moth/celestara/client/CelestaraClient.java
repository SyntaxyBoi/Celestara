package moth.celestara.client;

import moth.celestara.CelestaraMod;
import moth.celestara.client.sky.NightSkyPlanManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class CelestaraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CelestaraMod.LOGGER.info("[Celestara] Initializing client-only night sky visuals...");
        ClientCommandRegistrationCallback.EVENT.register(CelestaraClientCommands::register);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> NightSkyPlanManager.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> NightSkyPlanManager.clear());
    }
}
