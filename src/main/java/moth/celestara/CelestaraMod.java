package moth.celestara;

import moth.butterflyapi.ButterflyApi;
import moth.butterflyapi.mod.ModContext;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public final class CelestaraMod {
    public static final String MOD_ID = "celestara";
    public static final ModContext MOD = ButterflyApi.mod(MOD_ID, "Celestara");
    public static final Logger LOGGER = MOD.logger();

    private CelestaraMod() {
    }

    public static Identifier id(String path) {
        return MOD.id(path);
    }
}
