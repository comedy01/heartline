package dev.heartline.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import dev.heartline.mixin.GameRendererInvoker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

final class Projector {
    private Projector() {
    }

    static Projection create(Minecraft mc, float partial, int width, int height) {
        GameRenderer renderer = mc.gameRenderer;
        Camera camera = renderer.getMainCamera();
        GameRendererInvoker invoker = (GameRendererInvoker) renderer;
        double fov = invoker.heartline$getFov(camera, partial, true);
        PoseStack pose = new PoseStack();
        pose.last().pose().multiply(renderer.getProjectionMatrix(fov));
        invoker.heartline$bobHurt(pose, partial);
        if (mc.options.bobView) {
            invoker.heartline$bobView(pose, partial);
        }
        pose.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
        pose.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot() + 180.0F));
        return new MatrixProjection(pose.last().pose().copy(), camera.getPosition(), width, height);
    }

    private static final class MatrixProjection implements Projection {
        private final Matrix4f matrix;
        private final Vec3 camera;
        private final int width;
        private final int height;

        MatrixProjection(Matrix4f matrix, Vec3 camera, int width, int height) {
            this.matrix = matrix;
            this.camera = camera;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean project(double x, double y, double z, float[] out) {
            Vector4f point = new Vector4f((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1.0F);
            point.transform(matrix);
            if (point.w() <= 0.05F) {
                return false;
            }
            float ndcX = point.x() / point.w();
            float ndcY = point.y() / point.w();
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
