package dev.heartline.core;

public final class Palette {
    public static final int HIGH = 0xFF4CD04C;
    public static final int MID = 0xFFE8D03A;
    public static final int LOW = 0xFFE0443A;
    public static final int TRAIL = 0xFFF2E6C8;
    public static final int HEAL = 0xFF9CF09C;
    public static final int BACKGROUND = 0xB0101010;
    public static final int BORDER = 0xF0000000;
    public static final int EMPTY = 0xC0262A30;

    private Palette() {
    }

    public static int health(float fraction) {
        float f = Math.max(0.0F, Math.min(1.0F, fraction));
        if (f > 0.5F) {
            return lerp(MID, HIGH, (f - 0.5F) * 2.0F);
        }
        return lerp(LOW, MID, f * 2.0F);
    }

    public static int lerp(int from, int to, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(channel(from, 24) + (channel(to, 24) - channel(from, 24)) * k);
        int r = Math.round(channel(from, 16) + (channel(to, 16) - channel(from, 16)) * k);
        int g = Math.round(channel(from, 8) + (channel(to, 8) - channel(from, 8)) * k);
        int b = Math.round(channel(from, 0) + (channel(to, 0) - channel(from, 0)) * k);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int alpha(int color, float alpha) {
        int a = Math.round(channel(color, 24) * Math.max(0.0F, Math.min(1.0F, alpha)));
        return a << 24 | (color & 0xFFFFFF);
    }

    public static int darker(int color, float amount) {
        return lerp(color, color & 0xFF000000, amount);
    }

    private static int channel(int color, int shift) {
        return color >>> shift & 0xFF;
    }
}
