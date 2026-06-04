package moth.celestara.client.mixin;

import moth.celestara.client.sky.CelestaraSkyRenderer;
import moth.celestara.client.sky.CelestaraClientEventState;
import moth.celestara.CelestaraMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererSkyMixin {
    private static final Identifier CELESTARA_ECLIPSED_SUN = CelestaraMod.id("textures/environment/eclipsed_sun.png");
    private static final Identifier CELESTARA_ECLIPSED_MOON_PHASES = CelestaraMod.id("textures/environment/eclipsed_moon_phases.png");

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

    @ModifyArg(
            method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V", ordinal = 0),
            index = 1
    )
    private Identifier celestara$replaceSunTexture(Identifier original) {
        if (client.world != null && CelestaraClientEventState.isSolarEclipseActive(client.world)) {
            return CELESTARA_ECLIPSED_SUN;
        }
        return original;
    }

    @ModifyArg(
            method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V", ordinal = 1),
            index = 1
    )
    private Identifier celestara$replaceMoonTexture(Identifier original) {
        if (client.world != null && CelestaraClientEventState.isLunarEclipseActive(client.world)) {
            return CELESTARA_ECLIPSED_MOON_PHASES;
        }
        return original;
    }
}
