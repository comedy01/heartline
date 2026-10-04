package dev.heartline.core;

import com.google.gson.annotations.SerializedName;

public enum ColorMode implements Choice {
    @SerializedName("health")
    HEALTH("health"),
    @SerializedName("category")
    CATEGORY("category");

    private final String id;

    ColorMode(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
