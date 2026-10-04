package dev.heartline.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.heartline.config.HeartlineConfig;
import net.minecraft.client.KeyMapping;

import java.nio.file.Path;

public final class HeartlineClient {
    public static final String MOD_ID = "heartline";

    private static HeartlineConfig config = new HeartlineConfig();
    private static Path configPath;
    private static KeyMapping toggleKey;
    private static KeyMapping hideKey;

    private HeartlineClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(HeartlineConfig.FILE_NAME);
        config = HeartlineConfig.load(configPath);
    }

    public static KeyMapping createToggleKey() {
        return KeyFactory.create("key.heartline.toggle", InputConstants.UNKNOWN.getValue());
    }

    public static KeyMapping createHideKey() {
        return KeyFactory.create("key.heartline.hide", InputConstants.UNKNOWN.getValue());
    }

    public static void setKeys(KeyMapping toggle, KeyMapping hide) {
        toggleKey = toggle;
        hideKey = hide;
    }

    public static KeyMapping toggleKey() {
        return toggleKey;
    }

    public static KeyMapping hideKey() {
        return hideKey;
    }

    public static HeartlineConfig config() {
        return config;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
