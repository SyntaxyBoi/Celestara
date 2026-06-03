package moth.celestara.client.mixin;

import moth.celestara.client.sky.CelestaraSkyRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererSkyMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(
            method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At("TAIL")
    )
    private void celestara$renderNightSky(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta,
                                          Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        CelestaraSkyRenderer.render(matrices, client.world, tickDelta, thickFog);
    }
}
