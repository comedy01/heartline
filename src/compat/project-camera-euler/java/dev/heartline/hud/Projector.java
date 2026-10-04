package dev.heartline.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.heartline.mixin.GameRendererInvoker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

final class Projector {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private Projector() {
    }

    static Projection create(Minecraft mc, float partial, int width, int height) {
        GameRenderer renderer = mc.gameRenderer;
        Camera camera = renderer.getMainCamera();
        GameRendererInvoker invoker = (GameRendererInvoker) renderer;
        double fov = invoker.heartline$getFov(camera, partial, true);
        Matrix4f matrix = new Matrix4f(renderer.getProjectionMatrix(fov));
        PoseStack bob = new PoseStack();
        invoker.heartline$bobHurt(bob, partial);
        if (mc.options.bobView().get()) {
            invoker.heartline$bobView(bob, partial);
        }
        matrix.mul(bob.last().pose());
        Quaternionf viewRotation = new Quaternionf().rotationYXZ(
                (float) Math.PI - camera.getYRot() * DEG_TO_RAD, -camera.getXRot() * DEG_TO_RAD, 0.0F);
        matrix.mul(new Matrix4f().rotation(viewRotation.conjugate()));
        return new MatrixProjection(matrix, camera.getPosition(), width, height);
    }

    private static final class MatrixProjection implements Projection {
        private final Matrix4f matrix;
        private final Vec3 camera;
        private final int width;
        private final int height;
        private final Vector4f scratch = new Vector4f();

        MatrixProjection(Matrix4f matrix, Vec3 camera, int width, int height) {
            this.matrix = matrix;
            this.camera = camera;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean project(double x, double y, double z, float[] out) {
            scratch.set((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1.0F);
            matrix.transform(scratch);
            if (scratch.w <= 0.05F) {
                return false;
            }
            float ndcX = scratch.x / scratch.w;
            float ndcY = scratch.y / scratch.w;
            out[0] = (ndcX + 1.0F) * 0.5F * width;
            out[1] = (1.0F - ndcY) * 0.5F * height;
            return true;
        }

        @Override
        public double cameraX() {
            return camera.x;
        }

        @Override
        public double cameraY() {
            return camera.y;
        }

        @Override
        public double cameraZ() {
            return camera.z;
        }
    }
}
