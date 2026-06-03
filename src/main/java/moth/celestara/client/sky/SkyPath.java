package moth.celestara.client.sky;

import moth.butterflyapi.math.Vecs;
import net.minecraft.util.math.Vec3d;

public record SkyPath(Vec3d start, Vec3d control, Vec3d end) {
    public Vec3d positionAt(float progress) {
        double t = Math.max(0.0D, Math.min(1.0D, progress));
        double inv = 1.0D - t;
        Vec3d point = start.multiply(inv * inv)
                .add(control.multiply(2.0D * inv * t))
                .add(end.multiply(t * t));
        return Vecs.safeNormalize(point, end);
    }

    public Vec3d tangentAt(float progress) {
        double t = Math.max(0.0D, Math.min(1.0D, progress));
        Vec3d tangent = control.subtract(start).multiply(2.0D * (1.0D - t))
                .add(end.subtract(control).multiply(2.0D * t));
        return Vecs.safeNormalize(tangent, end.subtract(start));
    }
}
