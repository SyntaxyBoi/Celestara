package moth.celestara.client.mixin;

import moth.celestara.event.CelestaraEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldEclipseLightMixin {
    @Shadow
    private int ambientDarkness;

    @Inject(method = "calculateAmbientDarkness", at = @At("TAIL"))
    private void celestara$darkenSolarEclipse(CallbackInfo ci) {
        World world = (World) (Object) this;
        if (CelestaraEvents.isSolarEclipseActive(world)) {
            ambientDarkness = Math.max(ambientDarkness, 11);
        }
    }
}
