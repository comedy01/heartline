package dev.heartline.core;

import com.google.gson.annotations.SerializedName;

public enum PanelPosition implements Choice {
    @SerializedName("top_left")
    TOP_LEFT("top_left"),
    @SerializedName("top_center")
    TOP_CENTER("top_center"),
    @SerializedName("top_right")
    TOP_RIGHT("top_right"),
    @SerializedName("crosshair")
    CROSSHAIR("crosshair"),
    @SerializedName("off")
    OFF("off");

    private final String id;

    PanelPosition(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
