package dev.heartline.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.heartline.mixin.GameRendererInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class Projector {
    private Projector() {
    }

    static Projection create(Minecraft mc, float partial, int width, int height) {
        GameRenderer renderer = mc.gameRenderer;
        CameraRenderState camera = renderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f matrix = new Matrix4f(camera.projectionMatrix);
        PoseStack bob = new PoseStack();
        GameRendererInvoker invoker = (GameRendererInvoker) renderer;
        invoker.heartline$bobHurt(camera, bob);
        if (renderer.getGameRenderState().optionsRenderState.bobView) {
            invoker.heartline$bobView(camera, bob);
        }
        matrix.mul(bob.last().pose());
        matrix.mul(camera.viewRotationMatrix);
        return new MatrixProjection(matrix, camera.pos, width, height);
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
