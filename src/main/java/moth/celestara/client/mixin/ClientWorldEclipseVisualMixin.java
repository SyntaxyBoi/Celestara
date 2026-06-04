package moth.celestara.client.mixin;

import moth.celestara.client.sky.CelestaraClientEventState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldEclipseVisualMixin {
    private static final Vec3d SOLAR_SKY_COLOR = new Vec3d(0.003D, 0.004D, 0.009D);
    private static final Vec3d LUNAR_SKY_COLOR = new Vec3d(0.19D, 0.035D, 0.032D);
    private static final Vec3d SOLAR_CLOUD_COLOR = new Vec3d(0.020D, 0.022D, 0.030D);
    private static final Vec3d LUNAR_CLOUD_COLOR = new Vec3d(0.36D, 0.08D, 0.07D);

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void celestara$tintSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        ClientWorld world = (ClientWorld) (Object) this;
        if (CelestaraClientEventState.isSolarEclipseActive(world)) {
            cir.setReturnValue(mix(cir.getReturnValue(), SOLAR_SKY_COLOR, CelestaraClientEventState.solarEclipseFade(world)));
        } else if (CelestaraClientEventState.isLunarEclipseActive(world)) {
            cir.setReturnValue(mix(cir.getReturnValue(), LUNAR_SKY_COLOR, 0.48F * CelestaraClientEventState.lunarEclipseFade(world)));
        }
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void celestara$tintCloudsColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        ClientWorld world = (ClientWorld) (Object) this;
        if (CelestaraClientEventState.isSolarEclipseActive(world)) {
            cir.setReturnValue(mix(cir.getReturnValue(), SOLAR_CLOUD_COLOR, CelestaraClientEventState.solarEclipseFade(world)));
        } else if (CelestaraClientEventState.isLunarEclipseActive(world)) {
            cir.setReturnValue(mix(cir.getReturnValue(), LUNAR_CLOUD_COLOR, 0.42F * CelestaraClientEventState.lunarEclipseFade(world)));
        }
    }

    @Inject(method = "getSkyBrightness", at = @At("RETURN"), cancellable = true)
    private void celestara$solarEclipseSkyBrightness(float tickDelta, CallbackInfoReturnable<Float> cir) {
        ClientWorld world = (ClientWorld) (Object) this;
        if (CelestaraClientEventState.isSolarEclipseActive(world)) {
            cir.setReturnValue(Math.min(cir.getReturnValue(), 0.20F));
        }
    }

    @Inject(method = "method_23787", at = @At("RETURN"), cancellable = true)
    private void celestara$solarEclipseStarBrightness(float tickDelta, CallbackInfoReturnable<Float> cir) {
        ClientWorld world = (ClientWorld) (Object) this;
        if (CelestaraClientEventState.isSolarEclipseActive(world)) {
            float eclipseBrightness = 0.50F * CelestaraClientEventState.solarEclipseFade(world);
            cir.setReturnValue(Math.max(cir.getReturnValue(), eclipseBrightness));
        }
    }

    private static Vec3d mix(Vec3d original, Vec3d target, float amount) {
        double t = MathHelper.clamp(amount, 0.0F, 1.0F);
        return new Vec3d(
                MathHelper.lerp(t, original.x, target.x),
                MathHelper.lerp(t, original.y, target.y),
                MathHelper.lerp(t, original.z, target.z)
        );
    }
}
