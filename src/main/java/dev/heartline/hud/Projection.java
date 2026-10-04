package dev.heartline.hud;

public interface Projection {
    boolean project(double x, double y, double z, float[] out);

    double cameraX();

    double cameraY();

    double cameraZ();
}
