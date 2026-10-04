package dev.heartline.core;

import com.google.gson.annotations.SerializedName;

public enum HealthText implements Choice {
    @SerializedName("value")
    VALUE("value"),
    @SerializedName("percent")
    PERCENT("percent"),
    @SerializedName("hearts")
    HEARTS("hearts"),
    @SerializedName("off")
    OFF("off");

    private final String id;

    HealthText(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }
}
