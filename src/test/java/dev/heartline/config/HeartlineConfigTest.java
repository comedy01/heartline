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

    @Test
    void sizesSaveClampAndDropDefaults() throws IOException {
        Path file = dir.resolve("heartline.json");
        HeartlineConfig config = new HeartlineConfig();
        config.setSize("cataclysm:ignis", 1.8);
        config.setSize("minecraft:bat", 9.0);
        config.setSize("minecraft:cow", 1.0);
        assertEquals(2, config.sizedCount());
        assertEquals(HeartlineConfig.MAX_SCALE, config.size("minecraft:bat"), 0.001);
        assertEquals(1.0, config.size("minecraft:cow"), 0.001);
        config.save(file);

        HeartlineConfig loaded = HeartlineConfig.load(file);
        assertEquals(1.8, loaded.size("cataclysm:ignis"), 0.001);
        loaded.setSize("cataclysm:ignis", 1.0);
        assertEquals(1, loaded.sizedCount());
        loaded.resetToDefaults();
        assertEquals(1, loaded.sizedCount());
        loaded.clearSizes();
        assertEquals(0, loaded.sizedCount());
    }

    @Test
    void mobHeightsSaveClampAndCountOnce() throws IOException {
        Path file = dir.resolve("heartline.json");
        HeartlineConfig config = new HeartlineConfig();
        config.setHeight(9.0);
        assertEquals(HeartlineConfig.MAX_HEIGHT, config.height(), 0.001);
        config.setSize("cataclysm:ignis", 1.5);
        config.setMobHeight("cataclysm:ignis", 12.0);
        config.setMobHeight("alexsmobs:void_worm", 99.0);
        config.setMobHeight("minecraft:cow", 0.0);
        assertEquals(2, config.sizedCount());
        assertEquals(HeartlineConfig.MAX_MOB_HEIGHT, config.mobHeight("alexsmobs:void_worm"), 0.001);
        assertEquals(0.0, config.mobHeight("minecraft:cow"), 0.001);
        config.save(file);

        HeartlineConfig loaded = HeartlineConfig.load(file);
        assertEquals(12.0, loaded.mobHeight("cataclysm:ignis"), 0.001);
        loaded.resetToDefaults();
        assertEquals(12.0, loaded.mobHeight("cataclysm:ignis"), 0.001);
        loaded.clearSizes();
        assertEquals(0, loaded.sizedCount());
        assertEquals(0.0, loaded.mobHeight("cataclysm:ignis"), 0.001);
    }

    @Test
    void badSizesAndRowsFallBack() throws IOException {
        Path file = dir.resolve("heartline.json");
        Files.write(file, "{\"heartRows\":99,\"sizes\":{\"minecraft:bat\":null,\"\":1.5,\"minecraft:cow\":0.1}}"
                .getBytes(StandardCharsets.UTF_8));
        HeartlineConfig loaded = HeartlineConfig.load(file);
        assertEquals(HeartlineConfig.MAX_HEART_ROWS, loaded.heartRows());
        assertEquals(1, loaded.sizedCount());
        assertEquals(HeartlineConfig.MIN_SCALE, loaded.size("minecraft:cow"), 0.001);
    }

    @Test
    void namesSplitByPlayersAndMobs() {
        HeartlineConfig config = new HeartlineConfig();
        assertTrue(config.showsName(true));
        assertTrue(config.showsName(false));
        config.setNamePlayers(false);
        assertFalse(config.showsName(true));
        assertTrue(config.showsName(false));
        config.setNames(false);
        assertFalse(config.showsName(false));
    }

    @Test
    void cascadeHeartsSaves() throws IOException {
        Path file = dir.resolve("heartline.json");
        HeartlineConfig config = new HeartlineConfig();
        assertFalse(config.cascadeHearts());
        config.setCascadeHearts(true);
        config.save(file);
        assertTrue(HeartlineConfig.load(file).cascadeHearts());
    }
}
