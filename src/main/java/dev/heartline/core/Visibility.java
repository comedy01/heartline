package dev.heartline.core;

import com.google.gson.annotations.SerializedName;

public enum Visibility implements Choice {
    @SerializedName("always")
    ALWAYS("always"),
    @SerializedName("damaged")
    DAMAGED("damaged"),
    @SerializedName("combat")
    COMBAT("combat"),
    @SerializedName("looking")
    LOOKING("looking");

    private final String id;

    Visibility(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    public boolean shows(boolean damaged, boolean looking, int ticksSinceHurt, int fadeTicks) {
        if (looking) {
            return true;
        }
        switch (this) {
            case ALWAYS:
                return true;
            case DAMAGED:
                return damaged || ticksSinceHurt < fadeTicks;
            case COMBAT:
                return ticksSinceHurt < fadeTicks;
            default:
                return false;
        }
    }

    public float fade(boolean damaged, boolean looking, int ticksSinceHurt, int fadeTicks) {
        if (looking || this == ALWAYS || (this == DAMAGED && damaged)) {
            return 1.0F;
        }
        int left = fadeTicks - ticksSinceHurt;
        return Math.max(0.0F, Math.min(1.0F, left / 10.0F));
    }
}
