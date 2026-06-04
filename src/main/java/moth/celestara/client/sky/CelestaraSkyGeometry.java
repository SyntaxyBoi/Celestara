package moth.celestara.client.sky;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class CelestaraSkyGeometry {
    private CelestaraSkyGeometry() {
    }

    public static Vec3d skyPosition(Vec3d direction) {
        return direction.multiply(CelestaraVisualConstants.SKY_RADIUS);
    }

    public static Vec3d rotateX(Vec3d vector, float degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return rotateX(vector, cos, sin);
    }

    public static Vec3d rotateX(Vec3d vector, double cos, double sin) {
        return new Vec3d(
                vector.x,
                vector.y * cos - vector.z * sin,
                vector.y * sin + vector.z * cos
        );
    }

    public static void emitDiamond(Matrix4f matrix, BufferBuilder buffer, Vec3d center, Vec3d right, Vec3d up,
                                   float width, float height, int color, int alpha) {
        if (alpha <= 0 || width <= 0.0F || height <= 0.0F) {
            return;
        }

        Vec3d top = center.add(up.multiply(height));
        Vec3d rightPoint = center.add(right.multiply(width));
        Vec3d bottom = center.subtract(up.multiply(height));
        Vec3d left = center.subtract(right.multiply(width));
        emitTriangle(matrix, buffer, top, rightPoint, bottom, color, alpha);
        emitTriangle(matrix, buffer, top, bottom, left, color, alpha);
    }

    public static void emitPentagon(Matrix4f matrix, BufferBuilder buffer, Vec3d center, Vec3d right, Vec3d up,
                                    float radius, float rotationDegrees, int color, int alpha) {
        if (alpha <= 0 || radius <= 0.0F) {
            return;
        }

        double rotation = Math.toRadians(rotationDegrees) - Math.PI * 0.5D;
        Vec3d previous = pentagonPoint(center, right, up, radius, rotation);
        for (int point = 1; point <= CelestaraVisualConstants.HEAD_PENTAGON_POINTS; point++) {
            double angle = rotation + (Math.PI * 2.0D * point) / CelestaraVisualConstants.HEAD_PENTAGON_POINTS;
            Vec3d next = pentagonPoint(center, right, up, radius, angle);
            emitTriangle(matrix, buffer, center, previous, next, color, alpha);
            previous = next;
        }
    }

    public static void emitQuad(Matrix4f matrix, BufferBuilder buffer, Vec3d first, Vec3d second, Vec3d third,
                                Vec3d fourth, int color, int firstAlpha, int secondAlpha) {
        emitVertex(matrix, buffer, first, color, firstAlpha);
        emitVertex(matrix, buffer, second, color, firstAlpha);
        emitVertex(matrix, buffer, third, color, secondAlpha);
        emitVertex(matrix, buffer, first, color, firstAlpha);
        emitVertex(matrix, buffer, third, color, secondAlpha);
        emitVertex(matrix, buffer, fourth, color, secondAlpha);
    }

    public static void emitTriangle(Matrix4f matrix, BufferBuilder buffer, Vec3d first, Vec3d second, Vec3d third,
                                    int color, int alpha) {
        emitVertex(matrix, buffer, first, color, alpha);
        emitVertex(matrix, buffer, second, color, alpha);
        emitVertex(matrix, buffer, third, color, alpha);
    }

    public static void emitVertex(Matrix4f matrix, BufferBuilder buffer, Vec3d position, int color, int alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(
                        CelestialColorPalette.red(color),
                        CelestialColorPalette.green(color),
                        CelestialColorPalette.blue(color),
                        Math.max(0, Math.min(255, alpha))
                )
                .next();
    }

    private static Vec3d pentagonPoint(Vec3d center, Vec3d right, Vec3d up, float radius, double angle) {
        return center
                .add(right.multiply(Math.cos(angle) * radius))
                .add(up.multiply(Math.sin(angle) * radius));
    }
}
