package dev.heartline.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.heartline.core.BarStyle;
import dev.heartline.core.ColorMode;
import dev.heartline.core.HealthText;
import dev.heartline.core.PanelPosition;
import dev.heartline.core.Visibility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class HeartlineConfig {
    public static final String FILE_NAME = "heartline.json";

    public static final int MIN_RANGE = 8;
    public static final int MAX_RANGE = 64;
    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 2.0;
    public static final double MIN_HEIGHT = -0.5;
    public static final double MAX_HEIGHT = 1.5;
    public static final int MIN_FADE = 1;
    public static final int MAX_FADE = 30;
    public static final int MIN_PET_ALERT = 10;
    public static final int MAX_PET_ALERT = 60;

    private static final Logger LOGGER = LogManager.getLogger("heartline");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private boolean overhead = true;
    private Visibility visibility = Visibility.DAMAGED;
    private int range = 24;
    private boolean lineOfSight = true;
    private boolean hostile = true;
    private boolean neutral = true;
    private boolean passive = true;
    private boolean players = true;
    private boolean bosses = true;
    private boolean pets = true;
    private BarStyle style = BarStyle.SEGMENTED;
    private ColorMode colors = ColorMode.HEALTH;
    private double scale = 1.0;
    private double height = 0.0;
    private boolean names = true;
    private HealthText healthText = HealthText.VALUE;
    private boolean armor = true;
    private boolean trail = true;
    private int fadeSeconds = 6;
    private boolean damageNumbers = true;
    private boolean healNumbers = true;
    private boolean combo = true;
    private double numberScale = 1.0;
    private PanelPosition panel = PanelPosition.TOP_LEFT;
    private boolean hitsToKill = true;
    private boolean petAlert = true;
    private int petAlertPercent = 30;
    private List<String> hidden = new ArrayList<>();

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public boolean overhead() {
        return overhead;
    }

    public void setOverhead(boolean value) {
        overhead = value;
    }

    public Visibility visibility() {
        return visibility;
    }

    public void setVisibility(Visibility value) {
        visibility = value == null ? Visibility.DAMAGED : value;
    }

    public int range() {
        return range;
    }

    public void setRange(int value) {
        range = clamp(value, MIN_RANGE, MAX_RANGE);
    }

    public boolean lineOfSight() {
        return lineOfSight;
    }

    public void setLineOfSight(boolean value) {
        lineOfSight = value;
    }

    public boolean hostile() {
        return hostile;
    }

    public void setHostile(boolean value) {
        hostile = value;
    }

    public boolean neutral() {
        return neutral;
    }

    public void setNeutral(boolean value) {
        neutral = value;
    }

    public boolean passive() {
        return passive;
    }

    public void setPassive(boolean value) {
        passive = value;
    }

    public boolean players() {
        return players;
    }

    public void setPlayers(boolean value) {
        players = value;
    }

    public boolean bosses() {
        return bosses;
    }

    public void setBosses(boolean value) {
        bosses = value;
    }

    public boolean pets() {
        return pets;
    }

    public void setPets(boolean value) {
        pets = value;
    }

    public BarStyle style() {
        return style;
    }

    public void setStyle(BarStyle value) {
        style = value == null ? BarStyle.SEGMENTED : value;
    }

    public ColorMode colors() {
        return colors;
    }

    public void setColors(ColorMode value) {
        colors = value == null ? ColorMode.HEALTH : value;
    }

    public double scale() {
        return scale;
    }

    public void setScale(double value) {
        scale = clamp(value, MIN_SCALE, MAX_SCALE);
    }

    public double height() {
        return height;
    }

    public void setHeight(double value) {
        height = clamp(value, MIN_HEIGHT, MAX_HEIGHT);
    }

    public boolean names() {
        return names;
    }

    public void setNames(boolean value) {
        names = value;
    }

    public HealthText healthText() {
        return healthText;
    }

    public void setHealthText(HealthText value) {
        healthText = value == null ? HealthText.VALUE : value;
    }

    public boolean armor() {
        return armor;
    }

    public void setArmor(boolean value) {
        armor = value;
    }

    public boolean trail() {
        return trail;
    }

    public void setTrail(boolean value) {
        trail = value;
    }

    public int fadeSeconds() {
        return fadeSeconds;
    }

    public void setFadeSeconds(int value) {
        fadeSeconds = clamp(value, MIN_FADE, MAX_FADE);
    }

    public boolean damageNumbers() {
        return damageNumbers;
    }

    public void setDamageNumbers(boolean value) {
        damageNumbers = value;
    }

    public boolean healNumbers() {
        return healNumbers;
    }

    public void setHealNumbers(boolean value) {
        healNumbers = value;
    }

    public boolean combo() {
        return combo;
    }

    public void setCombo(boolean value) {
        combo = value;
    }

    public double numberScale() {
        return numberScale;
    }

    public void setNumberScale(double value) {
        numberScale = clamp(value, MIN_SCALE, MAX_SCALE);
    }

    public PanelPosition panel() {
        return panel;
    }

    public void setPanel(PanelPosition value) {
        panel = value == null ? PanelPosition.TOP_LEFT : value;
    }

    public boolean hitsToKill() {
        return hitsToKill;
    }

    public void setHitsToKill(boolean value) {
        hitsToKill = value;
    }

    public boolean petAlert() {
        return petAlert;
    }

    public void setPetAlert(boolean value) {
        petAlert = value;
    }

    public int petAlertPercent() {
        return petAlertPercent;
    }

    public void setPetAlertPercent(int value) {
        petAlertPercent = clamp(value, MIN_PET_ALERT, MAX_PET_ALERT);
    }

    public boolean isHidden(String type) {
        return hidden.contains(type);
    }

    public boolean toggleHidden(String type) {
        if (hidden.remove(type)) {
            return false;
        }
        hidden.add(type);
        return true;
    }

    public void setHidden(String type, boolean value) {
        if (value != hidden.contains(type)) {
            toggleHidden(type);
        }
    }

    public int hiddenCount() {
        return hidden.size();
    }

    public void clearHidden() {
        hidden.clear();
    }

    public void resetToDefaults() {
        List<String> keep = hidden;
        HeartlineConfig defaults = new HeartlineConfig();
        copyFrom(defaults);
        hidden = keep;
    }

    private void copyFrom(HeartlineConfig other) {
        enabled = other.enabled;
        overhead = other.overhead;
        visibility = other.visibility;
        range = other.range;
        lineOfSight = other.lineOfSight;
        hostile = other.hostile;
        neutral = other.neutral;
        passive = other.passive;
        players = other.players;
        bosses = other.bosses;
        pets = other.pets;
        style = other.style;
        colors = other.colors;
        scale = other.scale;
        height = other.height;
        names = other.names;
        healthText = other.healthText;
        armor = other.armor;
        trail = other.trail;
        fadeSeconds = other.fadeSeconds;
        damageNumbers = other.damageNumbers;
        healNumbers = other.healNumbers;
        combo = other.combo;
        numberScale = other.numberScale;
        panel = other.panel;
        hitsToKill = other.hitsToKill;
        petAlert = other.petAlert;
        petAlertPercent = other.petAlertPercent;
        hidden = new ArrayList<>(other.hidden);
    }

    private void sanitize() {
        setVisibility(visibility);
        setRange(range);
        setStyle(style);
        setColors(colors);
        setScale(scale);
        setHeight(height);
        setHealthText(healthText);
        setFadeSeconds(fadeSeconds);
        setNumberScale(numberScale);
        setPanel(panel);
        setPetAlertPercent(petAlertPercent);
        if (hidden == null) {
            hidden = new ArrayList<>();
        }
        hidden.removeIf(type -> type == null || type.isEmpty());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static HeartlineConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            HeartlineConfig fresh = new HeartlineConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            HeartlineConfig loaded = GSON.fromJson(reader, HeartlineConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            HeartlineConfig fresh = new HeartlineConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.write(temp, (GSON.toJson(this) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
