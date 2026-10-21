package dev.heartline.client.gui;

import dev.heartline.client.HeartlineClient;
import dev.heartline.client.MobTypes;
import dev.heartline.client.Texts;
import dev.heartline.config.HeartlineConfig;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class MobSizesScreen extends HeartlineOptionsScreen {
    private static final int WIDTH = 150;
    private static final double STEP = 0.1;
    private static final double HEIGHT_STEP = 0.5;

    private final Supplier<Screen> back;
    private final MobTypes.Mod mod;
    private final String query;
    private EditBox search;

    public MobSizesScreen(Screen lastScreen, Options options, Supplier<Screen> back) {
        this(lastScreen, options, back, null, "");
    }

    private MobSizesScreen(Screen lastScreen, Options options, Supplier<Screen> back, MobTypes.Mod mod,
                           String query) {
        super(lastScreen, options, mod == null
                ? Texts.translatable("heartline.options.mob_sizes.title")
                : Texts.literal(mod.name()));
        this.back = back;
        this.mod = mod;
        this.query = query;
    }

    @Override
    protected void addOptions() {
        if (mod == null) {
            addSearch();
            if (query.isEmpty()) {
                addMods();
            } else {
                addResults();
            }
        } else {
            addMobs();
        }
    }

    @Override
    protected void init() {
        super.init();
        if (search != null) {
            focus(this, search);
            Focus.on(search);
        }
    }

    private static boolean focus(ContainerEventHandler parent, GuiEventListener target) {
        for (GuiEventListener child : parent.children()) {
            if (child == target || child instanceof ContainerEventHandler container && focus(container, target)) {
                parent.setFocused(child);
                return true;
            }
        }
        return false;
    }

    private void addSearch() {
        search = new EditBox(minecraft.font, 0, 0, WIDTH * 2 + 10, 20,
                Texts.translatable("heartline.options.hidden_mobs.search"));
        search.setMaxLength(64);
        search.setValue(query);
        search.setSuggestion(query.isEmpty()
                ? Texts.translatable("heartline.options.hidden_mobs.search").getString() : null);
        search.setResponder(text -> ScreenOpener.open(minecraft,
                new MobSizesScreen(lastScreen, options, back, null, text)));
        addRow(search, null);
    }

    private void addResults() {
        HeartlineConfig config = HeartlineClient.config();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        int found = 0;
        List<Runnable> rows = new ArrayList<>();
        for (MobTypes.Mod each : MobTypes.byMod()) {
            for (MobTypes.Mob mob : each.mobs()) {
                if (mob.name().getString().toLowerCase(Locale.ROOT).contains(needle) || mob.id().contains(needle)) {
                    found++;
                    rows.add(() -> addMob(config, mob, Texts.translatable(
                            "heartline.options.hidden_mobs.result.tooltip", each.name(), mob.id())));
                }
            }
        }
        addHeader(found == 0
                ? Texts.translatable("heartline.options.hidden_mobs.no_results", query)
                : Texts.translatable("heartline.options.hidden_mobs.results", found));
        rows.forEach(Runnable::run);
    }

    private void addMods() {
        HeartlineConfig config = HeartlineClient.config();
        addRow(tooltip(button(Texts.translatable("heartline.options.mob_sizes.reset"), WIDTH, button -> {
            config.clearSizes();
            refresh();
        }), Texts.translatable("heartline.options.mob_sizes.reset.tooltip")), null);
        addHeader(Texts.translatable("heartline.options.hidden_mobs.mods"));
        List<AbstractWidget> buttons = new ArrayList<>();
        for (MobTypes.Mod each : MobTypes.byMod()) {
            buttons.add(tooltip(button(modLabel(config, each), WIDTH,
                            button -> ScreenOpener.open(minecraft, new MobSizesScreen(this, options, back, each, ""))),
                    Texts.translatable("heartline.options.hidden_mobs.mod.tooltip", each.mobs().size(), each.id())));
        }
        addPairs(buttons);
    }

    private void addMobs() {
        HeartlineConfig config = HeartlineClient.config();
        addRow(tooltip(button(Texts.translatable("heartline.options.mob_sizes.reset_mod"), WIDTH, button -> {
            for (MobTypes.Mob mob : mod.mobs()) {
                config.setSize(mob.id(), 1.0);
                config.setMobHeight(mob.id(), 0.0);
            }
            refresh();
        }), Texts.translatable("heartline.options.mob_sizes.reset_mod.tooltip")), null);
        addHeader(Texts.translatable("heartline.options.mob_sizes.mobs"));
        for (MobTypes.Mob mob : mod.mobs()) {
            addMob(config, mob, Texts.literal(mob.id()));
        }
    }

    private void addMob(HeartlineConfig config, MobTypes.Mob mob, Component tip) {
        addRow(tooltip(new HeartlineSettingsScreen.StepSlider(mob.name(), HeartlineConfig.MIN_SCALE,
                        HeartlineConfig.MAX_SCALE, STEP, config.size(mob.id()),
                        value -> HeartlineSettingsScreen.oneDecimal(value) + "x",
                        value -> config.setSize(mob.id(), value)), tip),
                tooltip(new HeartlineSettingsScreen.StepSlider("heartline.options.mob_sizes.height",
                        HeartlineConfig.MIN_MOB_HEIGHT, HeartlineConfig.MAX_MOB_HEIGHT, HEIGHT_STEP,
                        config.mobHeight(mob.id()), HeartlineSettingsScreen::oneDecimal,
                        value -> config.setMobHeight(mob.id(), value)),
                        Texts.translatable("heartline.options.mob_sizes.height.tooltip", mob.name())));
    }

    private void addPairs(List<AbstractWidget> widgets) {
        for (int i = 0; i < widgets.size(); i += 2) {
            addRow(widgets.get(i), i + 1 < widgets.size() ? widgets.get(i + 1) : null);
        }
    }

    private static Component modLabel(HeartlineConfig config, MobTypes.Mod mod) {
        int count = 0;
        for (MobTypes.Mob mob : mod.mobs()) {
            if (config.isResized(mob.id())) {
                count++;
            }
        }
        if (count == 0) {
            return Texts.literal(mod.name());
        }
        return Texts.translatable("heartline.options.mob_sizes.mod", mod.name(), count);
    }

    private void refresh() {
        ScreenOpener.open(minecraft, fresh());
    }

    private Screen fresh() {
        return new MobSizesScreen(lastScreen, options, back, mod, query);
    }

    @Override
    public void onClose() {
        HeartlineClient.saveConfig();
        ScreenOpener.open(minecraft, mod == null ? back.get() : ((MobSizesScreen) lastScreen).fresh());
    }
}
