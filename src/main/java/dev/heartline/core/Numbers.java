package dev.heartline.core;

import java.util.Locale;

public final class Numbers {
    private Numbers() {
    }

    public static String amount(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        if (Math.abs(rounded - Math.round(rounded)) < 0.05F) {
            return Integer.toString(Math.round(rounded));
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    public static String health(HealthText style, float health, float max) {
        float safeMax = Math.max(max, 0.0001F);
        float value = Math.max(health, 0.0F);
        switch (style) {
            case VALUE:
                return amount(value) + " / " + amount(max);
            case PERCENT:
                return Math.round(value / safeMax * 100.0F) + "%";
            case HEARTS:
                return amount(value / 2.0F) + " ❤";
            default:
                return "";
        }
    }

    public static int hitsToKill(float health, float perHit) {
        if (perHit <= 0.0F || health <= 0.0F) {
            return -1;
        }
        return (int) Math.ceil(health / perHit - 0.0001F);
    }
}
