package moth.celestara.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import moth.celestara.client.sky.CelestaraClientEventState;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererEclipseMixin {
    @Shadow
    private static float red;

    @Shadow
    private static float green;

    @Shadow
    private static float blue;

    @Inject(method = "render", at = @At("TAIL"))
    private static void celestara$darkenSolarEclipseFog(Camera camera, float tickDelta, ClientWorld world,
                                                        int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (!CelestaraClientEventState.isSolarEclipseActive(world)) {
            return;
        }

        red = 0.003F;
        green = 0.004F;
        blue = 0.009F;
        RenderSystem.clearColor(red, green, blue, 0.0F);
    }
}
