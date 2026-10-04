package dev.heartline.core;

public enum Category {
    HOSTILE(0xFFE0443A),
    NEUTRAL(0xFFE8B23A),
    PASSIVE(0xFF5CC85C),
    PLAYER(0xFF4FA3F0),
    PET(0xFFD070E8),
    BOSS(0xFFB03AE0);

    private final int color;

    Category(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }
}
