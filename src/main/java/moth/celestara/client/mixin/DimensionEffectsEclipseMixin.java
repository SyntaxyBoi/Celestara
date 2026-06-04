package moth.celestara.client.mixin;

import moth.celestara.client.sky.CelestaraClientEventState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionEffects.class)
public abstract class DimensionEffectsEclipseMixin {
    @Inject(method = "getFogColorOverride", at = @At("HEAD"), cancellable = true)
    private void celestara$removeSolarEclipseSunriseGradient(float skyAngle, float tickDelta,
                                                             CallbackInfoReturnable<float[]> cir) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (CelestaraClientEventState.isSolarEclipseActive(world)) {
            cir.setReturnValue(null);
        }
    }
}
