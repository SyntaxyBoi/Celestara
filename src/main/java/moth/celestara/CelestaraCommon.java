package moth.celestara;

import moth.celestara.event.CelestaraServerEvents;
import net.fabricmc.api.ModInitializer;

public final class CelestaraCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        CelestaraServerEvents.register();
    }
}
