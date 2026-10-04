package dev.heartline.core;

import com.google.gson.annotations.SerializedName;

public enum BarStyle implements Choice {
    @SerializedName("flat")
    FLAT("flat", 5),
    @SerializedName("segmented")
    SEGMENTED("segmented", 5),
    @SerializedName("slim")
    SLIM("slim", 2),
    @SerializedName("pip")
    PIP("pip", 6),
    @SerializedName("outline")
    OUTLINE("outline", 4),
    @SerializedName("glass")
    GLASS("glass", 5),
    @SerializedName("neon")
    NEON("neon", 4),
    @SerializedName("capsule")
    CAPSULE("capsule", 5),
    @SerializedName("gauge")
    GAUGE("gauge", 5),
    @SerializedName("bracket")
    BRACKET("bracket", 4);

    private final String id;
    private final int height;

    BarStyle(String id, int height) {
        this.id = id;
        this.height = height;
    }

    @Override
    public String id() {
        return id;
    }

    public int height() {
        return height;
    }
}
