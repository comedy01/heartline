package dev.heartline.client.gui;

import dev.heartline.client.HeartlineClient;
import dev.heartline.client.MobTypes;
import dev.heartline.client.Texts;
import dev.heartline.config.HeartlineConfig;
import net.minecraft.ChatFormatting;
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

public final class HiddenMobsScreen extends HeartlineOptionsScreen {
    private static final int WIDTH = 150;

    private final Supplier<Screen> back;
    private final MobTypes.Mod mod;
    private final String query;
    private EditBox search;

    public HiddenMobsScreen(Screen lastScreen, Options options, Supplier<Screen> back) {
        this(lastScreen, options, back, null, "");
    }

    private HiddenMobsScreen(Screen lastScreen, Options options, Supplier<Screen> back, MobTypes.Mod mod,
                             String query) {
        super(lastScreen, options, mod == null
                ? Texts.translatable("heartline.options.hidden_mobs.title")
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
                new HiddenMobsScreen(lastScreen, options, back, null, text)));
        addRow(search, null);
    }

    private void addResults() {
        HeartlineConfig config = HeartlineClient.config();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<AbstractWidget> buttons = new ArrayList<>();
        for (MobTypes.Mod each : MobTypes.byMod()) {
            for (MobTypes.Mob mob : each.mobs()) {
                if (mob.name().getString().toLowerCase(Locale.ROOT).contains(needle) || mob.id().contains(needle)) {
                    buttons.add(mobToggle(config, mob, Texts.translatable(
                            "heartline.options.hidden_mobs.result.tooltip", each.name(), mob.id())));
                }
            }
        }
        addHeader(buttons.isEmpty()
                ? Texts.translatable("heartline.options.hidden_mobs.no_results", query)
                : Texts.translatable("heartline.options.hidden_mobs.results", buttons.size()));
        addPairs(buttons);
    }

    private void addMods() {
        HeartlineConfig config = HeartlineClient.config();
        addRow(tooltip(button(Texts.translatable("heartline.options.hidden_mobs.show_all"), WIDTH, button -> {
            config.clearHidden();
            refresh();
        }), Texts.translatable("heartline.options.hidden_mobs.show_all.tooltip")), null);
        addHeader(Texts.translatable("heartline.options.hidden_mobs.mods"));
        List<AbstractWidget> buttons = new ArrayList<>();
        for (MobTypes.Mod each : MobTypes.byMod()) {
            buttons.add(tooltip(button(modLabel(config, each), WIDTH,
                            button -> ScreenOpener.open(minecraft, new HiddenMobsScreen(this, options, back, each, ""))),
                    Texts.translatable("heartline.options.hidden_mobs.mod.tooltip", each.mobs().size(), each.id())));
        }
        addPairs(buttons);
    }

    private void addMobs() {
        HeartlineConfig config = HeartlineClient.config();
        addRow(button(Texts.translatable("heartline.options.hidden_mobs.hide_mod"), WIDTH, button -> {
                    setAll(config, true);
                    refresh();
                }),
                button(Texts.translatable("heartline.options.hidden_mobs.show_mod"), WIDTH, button -> {
                    setAll(config, false);
                    refresh();
                }));
        addHeader(Texts.translatable("heartline.options.hidden_mobs.mobs"));
        List<AbstractWidget> buttons = new ArrayList<>();
        for (MobTypes.Mob mob : mod.mobs()) {
            buttons.add(mobToggle(config, mob, Texts.literal(mob.id())));
        }
        addPairs(buttons);
    }

    private AbstractWidget mobToggle(HeartlineConfig config, MobTypes.Mob mob, Component tip) {
        return tooltip(button(mobLabel(config, mob), WIDTH, button -> {
            config.toggleHidden(mob.id());
            button.setMessage(mobLabel(config, mob));
        }), tip);
    }

    private void addPairs(List<AbstractWidget> buttons) {
        for (int i = 0; i < buttons.size(); i += 2) {
            addRow(buttons.get(i), i + 1 < buttons.size() ? buttons.get(i + 1) : null);
        }
    }

    private void setAll(HeartlineConfig config, boolean hidden) {
        for (MobTypes.Mob mob : mod.mobs()) {
            config.setHidden(mob.id(), hidden);
        }
    }

    private static Component modLabel(HeartlineConfig config, MobTypes.Mod mod) {
        int count = 0;
        for (MobTypes.Mob mob : mod.mobs()) {
            if (config.isHidden(mob.id())) {
                count++;
            }
        }
        if (count == 0) {
            return Texts.literal(mod.name());
        }
        return Texts.translatable("heartline.options.hidden_mobs.mod", mod.name(), count);
    }

    private static Component mobLabel(HeartlineConfig config, MobTypes.Mob mob) {
        Component state = config.isHidden(mob.id())
                ? Texts.translatable("heartline.options.hidden_mobs.hidden").withStyle(ChatFormatting.RED)
                : Texts.translatable("heartline.options.hidden_mobs.shown");
        return Texts.translatable("options.generic_value", mob.name(), state);
    }

    private void refresh() {
        ScreenOpener.open(minecraft, fresh());
    }

    private Screen fresh() {
        return new HiddenMobsScreen(lastScreen, options, back, mod, query);
    }

    @Override
    public void onClose() {
        HeartlineClient.saveConfig();
        ScreenOpener.open(minecraft, mod == null ? back.get() : ((HiddenMobsScreen) lastScreen).fresh());
    }
}
