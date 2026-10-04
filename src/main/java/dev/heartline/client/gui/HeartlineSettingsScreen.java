package dev.heartline.client.gui;

import dev.heartline.client.HeartlineClient;
import dev.heartline.client.Texts;
import dev.heartline.config.HeartlineConfig;
import dev.heartline.core.BarStyle;
import dev.heartline.core.Choice;
import dev.heartline.core.ColorMode;
import dev.heartline.core.HealthText;
import dev.heartline.core.PanelPosition;
import dev.heartline.core.Visibility;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

public final class HeartlineSettingsScreen extends HeartlineOptionsScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public HeartlineSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Texts.translatable("heartline.options.title"));
    }

    @Override
    protected void addOptions() {
        HeartlineConfig config = HeartlineClient.config();

        addRow(toggle("heartline.options.enabled", config::enabled, config::setEnabled), keyBindsButton());

        addHeader(Texts.translatable("heartline.options.section.bars"));
        addRow(
                toggle("heartline.options.overhead", config::overhead, config::setOverhead),
                cycle("heartline.options.panel", PanelPosition.values(), config::panel, config::setPanel));
        addRow(
                cycle("heartline.options.visibility", Visibility.values(), config::visibility, config::setVisibility),
                slider("heartline.options.range", HeartlineConfig.MIN_RANGE, HeartlineConfig.MAX_RANGE, 4,
                        config.range(), value -> Integer.toString((int) value), value -> config.setRange((int) value)));
        addRow(
                toggle("heartline.options.line_of_sight", config::lineOfSight, config::setLineOfSight),
                slider("heartline.options.fade", HeartlineConfig.MIN_FADE, HeartlineConfig.MAX_FADE, 1,
                        config.fadeSeconds(), value -> (int) value + "s", value -> config.setFadeSeconds((int) value)));
        addRow(
                cycle("heartline.options.style", BarStyle.values(), config::style, config::setStyle),
                cycle("heartline.options.colors", ColorMode.values(), config::colors, config::setColors));
        addRow(
                slider("heartline.options.scale", HeartlineConfig.MIN_SCALE, HeartlineConfig.MAX_SCALE, 0.1,
                        config.scale(), HeartlineSettingsScreen::oneDecimal, config::setScale),
                slider("heartline.options.height", HeartlineConfig.MIN_HEIGHT, HeartlineConfig.MAX_HEIGHT, 0.1,
                        config.height(), HeartlineSettingsScreen::oneDecimal, config::setHeight));
        addRow(
                toggle("heartline.options.names", config::names, config::setNames),
                cycle("heartline.options.health_text", HealthText.values(), config::healthText, config::setHealthText));
        addRow(
                toggle("heartline.options.armor", config::armor, config::setArmor),
                toggle("heartline.options.trail", config::trail, config::setTrail));

        addHeader(Texts.translatable("heartline.options.section.mobs"));
        addRow(
                toggle("heartline.options.hostile", config::hostile, config::setHostile),
                toggle("heartline.options.neutral", config::neutral, config::setNeutral));
        addRow(
                toggle("heartline.options.passive", config::passive, config::setPassive),
                toggle("heartline.options.players", config::players, config::setPlayers));
        addRow(
                toggle("heartline.options.bosses", config::bosses, config::setBosses),
                toggle("heartline.options.pets", config::pets, config::setPets));
        addRow(hiddenButton(config), null);

        addHeader(Texts.translatable("heartline.options.section.numbers"));
        addRow(
                toggle("heartline.options.damage_numbers", config::damageNumbers, config::setDamageNumbers),
                toggle("heartline.options.heal_numbers", config::healNumbers, config::setHealNumbers));
        addRow(
                toggle("heartline.options.combo", config::combo, config::setCombo),
                slider("heartline.options.number_scale", HeartlineConfig.MIN_SCALE, HeartlineConfig.MAX_SCALE,
                        0.1, config.numberScale(), HeartlineSettingsScreen::oneDecimal, config::setNumberScale));

        addHeader(Texts.translatable("heartline.options.section.panel"));
        addRow(
                toggle("heartline.options.hits_to_kill", config::hitsToKill, config::setHitsToKill),
                toggle("heartline.options.pet_alert", config::petAlert, config::setPetAlert));
        addRow(
                slider("heartline.options.pet_alert_percent", HeartlineConfig.MIN_PET_ALERT,
                        HeartlineConfig.MAX_PET_ALERT, 5, config.petAlertPercent(), value -> (int) value + "%",
                        value -> config.setPetAlertPercent((int) value)),
                resetButton(config));
    }

    @Override
    public void removed() {
        super.removed();
        HeartlineClient.saveConfig();
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private AbstractWidget hiddenButton(HeartlineConfig config) {
        return tooltip(button(Texts.translatable("heartline.options.hidden", config.hiddenCount()), WIDTH,
                        button -> ScreenOpener.open(minecraft, new HiddenMobsScreen(this, options,
                                () -> new HeartlineSettingsScreen(lastScreen, options)))),
                Texts.translatable("heartline.options.hidden.tooltip"));
    }

    private AbstractWidget keyBindsButton() {
        return tooltip(button(Texts.translatable("heartline.options.keys"), WIDTH,
                        button -> ScreenOpener.open(minecraft, KeyBinds.screen(this, options))),
                Texts.translatable("heartline.options.keys.tooltip"));
    }

    private AbstractWidget resetButton(HeartlineConfig config) {
        return tooltip(button(Texts.translatable("heartline.options.reset"), WIDTH, button -> {
            config.resetToDefaults();
            ScreenOpener.open(minecraft, new HeartlineSettingsScreen(lastScreen, options));
        }), Texts.translatable("heartline.options.reset.tooltip"));
    }

    private AbstractWidget toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return tooltip(button(toggleLabel(key, getter.getAsBoolean()), WIDTH, button -> {
            setter.accept(!getter.getAsBoolean());
            button.setMessage(toggleLabel(key, getter.getAsBoolean()));
        }), Texts.translatable(key + ".tooltip"));
    }

    private static Component toggleLabel(String key, boolean on) {
        Component state = Texts.translatable(on ? "options.on" : "options.off");
        return Texts.translatable("options.generic_value", Texts.translatable(key), state);
    }

    private <T extends Enum<T> & Choice> AbstractWidget cycle(
            String key, T[] values, Supplier<T> getter, Consumer<T> setter) {
        return tooltip(button(cycleLabel(key, getter.get()), WIDTH, button -> {
            T next = values[(getter.get().ordinal() + 1) % values.length];
            setter.accept(next);
            button.setMessage(cycleLabel(key, getter.get()));
        }), Texts.translatable(key + ".tooltip"));
    }

    private static Component cycleLabel(String key, Choice value) {
        return Texts.translatable("options.generic_value", Texts.translatable(key),
                Texts.translatable(key + "." + value.id()));
    }

    private AbstractWidget slider(String key, double min, double max, double step, double initial,
                                  DoubleFunction<String> format, DoubleConsumer onChange) {
        return tooltip(new StepSlider(key, min, max, step, initial, format, onChange),
                Texts.translatable(key + ".tooltip"));
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer onChange;

        StepSlider(String captionKey, double min, double max, double step, double initial,
                   DoubleFunction<String> format, DoubleConsumer onChange) {
            super(0, 0, WIDTH, HEIGHT, Texts.empty(), 0.0);
            this.captionKey = captionKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
            updateMessage();
        }

        private double snap(double raw) {
            double clamped = Math.max(min, Math.min(max, raw));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        private double current() {
            return snap(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            Component shown = Texts.literal(format.apply(current()));
            setMessage(Texts.translatable("options.generic_value", Texts.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
