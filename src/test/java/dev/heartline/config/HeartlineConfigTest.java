package dev.heartline.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.heartline.core.PanelPosition;
import dev.heartline.core.Visibility;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HeartlineConfigTest {
    @TempDir
    Path dir;

    @Test
    void savesAndLoads() throws IOException {
        Path file = dir.resolve("heartline.json");
        HeartlineConfig config = HeartlineConfig.load(file);
        config.setVisibility(Visibility.COMBAT);
        config.setPanel(PanelPosition.CROSSHAIR);
        config.setRange(40);
        config.toggleHidden("minecraft:bat");
        config.save(file);

        HeartlineConfig loaded = HeartlineConfig.load(file);
        assertEquals(Visibility.COMBAT, loaded.visibility());
        assertEquals(PanelPosition.CROSSHAIR, loaded.panel());
        assertEquals(40, loaded.range());
        assertTrue(loaded.isHidden("minecraft:bat"));
        assertTrue(Files.readString(file).contains("\"visibility\": \"combat\""));
    }

    @Test
    void badValuesFallBack() throws IOException {
        Path file = dir.resolve("heartline.json");
        Files.write(file, "{\"visibility\":\"nope\",\"range\":999,\"scale\":-3,\"hidden\":null}".getBytes(StandardCharsets.UTF_8));
        HeartlineConfig loaded = HeartlineConfig.load(file);
        assertEquals(Visibility.DAMAGED, loaded.visibility());
        assertEquals(HeartlineConfig.MAX_RANGE, loaded.range());
        assertEquals(HeartlineConfig.MIN_SCALE, loaded.scale(), 0.001);
        assertEquals(0, loaded.hiddenCount());
    }

    @Test
    void brokenFileIsMovedAside() throws IOException {
        Path file = dir.resolve("heartline.json");
        Files.write(file, "{not json".getBytes(StandardCharsets.UTF_8));
        HeartlineConfig loaded = HeartlineConfig.load(file);
        assertTrue(loaded.enabled());
        assertTrue(Files.exists(dir.resolve("heartline.json.broken")));
    }

    @Test
    void resetKeepsHiddenTypes() {
        HeartlineConfig config = new HeartlineConfig();
        config.toggleHidden("minecraft:bat");
        config.setEnabled(false);
        config.resetToDefaults();
        assertTrue(config.enabled());
        assertTrue(config.isHidden("minecraft:bat"));
        assertFalse(config.toggleHidden("minecraft:bat"));
    }

    @Test
    void setHiddenOnlyChangesWhenNeeded() {
        HeartlineConfig config = new HeartlineConfig();
        config.setHidden("cataclysm:ignis", true);
        config.setHidden("cataclysm:ignis", true);
        assertEquals(1, config.hiddenCount());
        config.setHidden("cataclysm:ignis", false);
        config.setHidden("cataclysm:ignis", false);
        assertEquals(0, config.hiddenCount());
    }

    @Test
    void oldConfigsKeepBarsAndBossesOn() throws IOException {
        Path file = dir.resolve("heartline.json");
        Files.write(file, "{\"hostile\":false}".getBytes(StandardCharsets.UTF_8));
        HeartlineConfig config = HeartlineConfig.load(file);
        assertFalse(config.hostile());
        assertTrue(config.overhead());
        assertTrue(config.bosses());
    }
}
