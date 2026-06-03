package moth.celestara.client.sky;

import moth.butterflyapi.math.Basis3;
import net.minecraft.util.math.Vec3d;

public record StarVisualData(
        Vec3d direction,
        Basis3 basis,
        float size,
        float baseBrightness,
        int color,
        float baseRotation,
        float twinklePhase,
        float twinkleAmplitude,
        float twinkleSpeed,
        float oscillationPhase,
        float oscillationSpeed,
        float maxRotationOffset,
        float layerSpeed
) {
}
