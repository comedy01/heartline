package dev.heartline.selftest;

import dev.heartline.client.Entities;
import dev.heartline.client.Ground;
import dev.heartline.client.HeartlineClient;
import dev.heartline.client.MobTypes;
import dev.heartline.client.Tracked;
import dev.heartline.client.Tracker;
import dev.heartline.client.gui.HeartlineSettingsScreen;
import dev.heartline.client.gui.HiddenMobsScreen;
import dev.heartline.config.HeartlineConfig;
import dev.heartline.core.BarStyle;
import dev.heartline.core.Category;
import dev.heartline.core.PanelPosition;
import dev.heartline.core.Popup;
import dev.heartline.core.Visibility;
import dev.heartline.hud.HeartlineHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class HeartlineSelfTest {
    private static final String WORLD = "heartline-selftest";
    private static final int TIMEOUT = 2400;

    private static volatile int frames;

    private final List<Step> steps = new ArrayList<>();
    private final List<String> shots = new ArrayList<>();
    private final Set<String> existingShots = new HashSet<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int seenFrames;
    private int patience;

    private LivingEntity zombie;
    private LivingEntity wolf;
    private float healthBefore;
    private float swordDamage;

    private record Step(int delay, BooleanSupplier ready, Runnable action) {
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 40) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                ServerCompat.hideChat(mc);
                rememberShots(mc);
                plan(mc);
                delay = steps.get(0).delay();
                log("creating world");
                deleteWorld(mc, WORLD);
                Worlds.create(mc, WORLD, true, 0L);
            }
            return;
        }
        int drawn = frames;
        if (drawn == seenFrames) {
            return;
        }
        seenFrames = drawn;
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready().getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action().run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay();
        }
    }

    public static void frame() {
        frames++;
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private void when(BooleanSupplier ready, Runnable action) {
        steps.add(new Step(0, ready, action));
    }

    private void within(int ticks, BooleanSupplier ready, Runnable action) {
        then(1, () -> patience = ticks);
        when(() -> ready.getAsBoolean() || --patience < 0, action);
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        nameShots(mc);
        if (failure == null) {
            log("ALL CHECKS PASSED");
        } else {
            log("FAILED: " + failure);
            failure.printStackTrace();
        }
        HeartlineConfig config = HeartlineClient.config();
        config.resetToDefaults();
        config.clearHidden();
        HeartlineClient.saveConfig();
        mc.stop();
    }

    private void plan(Minecraft mc) {
        HeartlineConfig config = HeartlineClient.config();
        int floor = ServerCompat.FLOOR;

        when(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null, () -> log("world loaded"));
        then(40, () -> {
            config.resetToDefaults();
            config.clearHidden();
            config.setVisibility(Visibility.ALWAYS);
            run(mc, "time set 6000");
            run(mc, "gamerule advance_time false");
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 5");
            run(mc, "item replace entity @p weapon.mainhand with minecraft:iron_sword");
            run(mc, "summon minecraft:husk 0.5 " + floor + " 3.2 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
            int[] uuid = uuidInts(mc.player.getUUID());
            run(mc, String.format(Locale.ROOT,
                    "summon minecraft:wolf -2.5 %d 4.5 {NoAI:1b,PersistenceRequired:1b,Owner:[I;%d,%d,%d,%d]}",
                    floor, uuid[0], uuid[1], uuid[2], uuid[3]));
        });
        within(60, () -> find(mc, "minecraft:husk") != null && find(mc, "minecraft:wolf") != null, () -> {
            zombie = find(mc, "minecraft:husk");
            wolf = find(mc, "minecraft:wolf");
            check(zombie != null, "the zombie did not show up on the client");
            check(wolf != null, "the wolf did not show up on the client");
        });
        within(40, () -> HeartlineHud.drew(zombie.getId()) && HeartlineHud.drew(wolf.getId()), () -> {
            check(Tracker.get(zombie.getId()) != null, "the zombie is not tracked");
            check(HeartlineHud.drew(zombie.getId()), "no health bar was drawn above the zombie");
            check(HeartlineHud.drew(wolf.getId()), "no health bar was drawn above the wolf");
            check(Entities.category(wolf, mc.player) == Category.PET, "the tamed wolf is not seen as a pet");
            check(Entities.category(zombie, mc.player) == Category.HOSTILE, "the zombie is not seen as hostile");
            check(Tracker.lookId() == zombie.getId(), "the crosshair is not on the zombie: " + Tracker.lookId());
            check(HeartlineHud.panelDrawn(), "the target panel was not drawn");
            screenshot(mc, "bars");
        });
        then(10, () -> config.setStyle(BarStyle.PIP));
        then(5, () -> screenshot(mc, "style-pip"));
        then(5, () -> config.setStyle(BarStyle.OUTLINE));
        then(5, () -> screenshot(mc, "style-outline"));
        then(5, () -> config.setStyle(BarStyle.SEGMENTED));
        within(40, () -> mc.player.getAttackStrengthScale(0.5F) >= 1.0F, () -> {
            healthBefore = zombie.getHealth();
            attack(mc, zombie);
        });
        within(40, () -> zombie.getHealth() < healthBefore && popupFor(zombie) != null && HeartlineHud.popupsDrawn() > 0,
                () -> {
            float dealt = healthBefore - zombie.getHealth();
            check(dealt > 0.0F, "the sword hit did not hurt the zombie");
            Popup popup = popupFor(zombie);
            check(popup != null, "no damage number showed up");
            check(popup.kind() == Popup.Kind.DAMAGE, "a normal hit showed as " + popup.kind());
            check(Math.abs(popup.amount() - dealt) < 0.05F, "the number " + popup.amount() + " is not the damage " + dealt);
            swordDamage = Entities.attackDamage(mc.player);
            check(Math.abs(swordDamage - dealt) < 0.5F,
                    "the hits-to-kill estimate (" + swordDamage + ") is not close to the iron sword's damage (" + dealt + ")");
            check(HeartlineHud.popupsDrawn() > 0, "the damage number was not drawn");
            screenshot(mc, "hit");
        });
        within(40, () -> mc.player.getAttackStrengthScale(0.5F) > 0.95F,
                () -> run(mc, "tp @p 0.5 " + (floor + 2.5) + " 0.5 0 12"));
        within(40, () -> !Ground.on(mc.player) && mc.player.fallDistance > 0.3F, () -> {
            check(Entities.canCrit(mc.player, zombie), "falling with a full swing does not count as a crit: ground="
                    + Ground.on(mc.player) + " fall=" + mc.player.fallDistance + " strength="
                    + mc.player.getAttackStrengthScale(0.5F) + " y=" + mc.player.getY());
            healthBefore = zombie.getHealth();
            attack(mc, zombie);
        });
        within(40, () -> zombie.getHealth() < healthBefore, () -> {
            Popup popup = popupFor(zombie);
            check(popup != null, "no number for the critical hit");
            check(popup.kind() == Popup.Kind.CRIT, "the critical hit showed as " + popup.kind());
            check(popup.hits() == 2, "the two quick hits did not stack into one combo number: " + popup.hits());
            check(Math.abs(popup.amount() - (20.0F - zombie.getHealth())) < 0.05F,
                    "the combo total " + popup.amount() + " is not the damage taken");
            screenshot(mc, "crit-combo");
        });
        then(30, () -> config.setStyle(BarStyle.GLASS));
        then(5, () -> screenshot(mc, "style-glass"));
        then(1, () -> config.setStyle(BarStyle.NEON));
        then(5, () -> screenshot(mc, "style-neon"));
        then(1, () -> config.setStyle(BarStyle.CAPSULE));
        then(5, () -> screenshot(mc, "style-capsule"));
        then(1, () -> config.setStyle(BarStyle.GAUGE));
        then(5, () -> screenshot(mc, "style-gauge"));
        then(1, () -> config.setStyle(BarStyle.BRACKET));
        then(5, () -> screenshot(mc, "style-bracket"));
        then(1, () -> config.setStyle(BarStyle.SEGMENTED));
        then(5, () -> run(mc, "item replace entity @p weapon.mainhand with minecraft:air"));
        within(20, () -> Entities.attackDamage(mc.player) < swordDamage - 0.5F, () -> {
            float handDamage = Entities.attackDamage(mc.player);
            check(handDamage < swordDamage,
                    "switching to an empty hand should lower the live hits-to-kill estimate: sword=" + swordDamage
                            + " hand=" + handDamage);
            check(handDamage > 0.0F, "an empty hand should still deal some damage: " + handDamage);
        });
        then(10, () -> config.setOverhead(false));
        then(3, () -> {
            check(!HeartlineHud.drew(zombie.getId()) && !HeartlineHud.drew(wolf.getId()),
                    "Bars Above Mobs is off but a bar was still drawn");
            screenshot(mc, "no-overhead");
            config.setOverhead(true);
        });
        then(10, () -> {
            check(Tracker.toggleHidden(zombie), "the hide toggle did not hide the zombie type");
            check(config.isHidden("minecraft:husk"), "the zombie type was not saved as hidden");
        });
        then(3, () -> {
            check(!HeartlineHud.drew(zombie.getId()), "a hidden mob type still got a bar");
            check(HeartlineHud.drew(wolf.getId()), "hiding zombies also hid the wolf");
            screenshot(mc, "hidden");
            check(!Tracker.toggleHidden(zombie), "the hide toggle did not show the zombie type again");
            run(mc, "fill -3 " + floor + " 1 3 " + (floor + 3) + " 1 minecraft:stone");
        });
        within(40, () -> !HeartlineHud.drew(zombie.getId()), () -> {
            check(!HeartlineHud.drew(zombie.getId()), "the zombie's bar showed through the wall");
            screenshot(mc, "wall");
            run(mc, "fill -3 " + floor + " 1 3 " + (floor + 3) + " 1 minecraft:air");
        });
        within(40, () -> HeartlineHud.drew(zombie.getId()), () -> {
            check(HeartlineHud.drew(zombie.getId()), "the zombie's bar did not come back after the wall was removed");
            IntegratedServer petServer = mc.getSingleplayerServer();
            petServer.execute(() -> {
                Entity entity = petServer.overworld().getEntity(wolf.getUUID());
                if (entity instanceof LivingEntity living) {
                    living.setHealth(1.0F);
                }
            });
        });
        within(40, () -> Tracker.petWarning() != null, () -> {
            check(Tracker.petWarning() != null, "no warning when the pet got low on health");
            check(Tracker.petWarning().contains("%"), "the pet warning has no percentage: " + Tracker.petWarning());
            screenshot(mc, "pet-warning");
            config.setPanel(PanelPosition.CROSSHAIR);
        });
        then(5, () -> {
            check(HeartlineHud.panelDrawn(), "the crosshair panel was not drawn");
            screenshot(mc, "crosshair-panel");
            config.setPanel(PanelPosition.TOP_LEFT);
            config.setVisibility(Visibility.DAMAGED);
            Screens.open(mc, new HeartlineSettingsScreen(null, mc.options));
        });
        then(10, () -> {
            Screen screen = Screens.current(mc);
            check(screen instanceof HeartlineSettingsScreen, "the settings screen did not open");
            screenshot(mc, "settings");
            click(mc, find(screen, "heartline.options.visibility"));
            check(config.visibility() == Visibility.COMBAT, "the Show button did not cycle: " + config.visibility());
        });
        then(5, () -> {
            scroll(Screens.current(mc), true);
        });
        then(5, () -> {
            screenshot(mc, "settings-bottom");
            List<MobTypes.Mod> mods = MobTypes.byMod();
            check(!mods.isEmpty() && mods.get(0).id().equals("minecraft"), "vanilla is not the first mod in the list");
            List<String> vanilla = new ArrayList<>();
            for (MobTypes.Mob mob : mods.get(0).mobs()) {
                vanilla.add(mob.id());
            }
            check(vanilla.contains("minecraft:husk") && vanilla.contains("minecraft:bat"),
                    "the mob list is missing vanilla mobs: " + vanilla);
            check(!vanilla.contains("minecraft:player") && !vanilla.contains("minecraft:item"),
                    "the mob list has entities that never get a bar: " + vanilla);
            Screens.open(mc, new HiddenMobsScreen(null, mc.options, () -> null));
        });
        then(10, () -> {
            Screen screen = Screens.current(mc);
            check(screen instanceof HiddenMobsScreen, "the hidden mobs screen did not open");
            screenshot(mc, "hidden-mobs");
            EditBox search = findBox(screen);
            check(search != null, "the hidden mobs screen has no search box");
            search.setValue("husk");
        });
        then(10, () -> {
            Screen screen = Screens.current(mc);
            check(find(screen, "entity.minecraft.husk") != null, "searching for husk did not list the husk");
            check(findText(screen, "Minecraft", false) == null, "the search results still show the mod list");
            EditBox search = findBox(screen);
            check(search != null && search.isFocused(), "the search box lost focus after typing");
            screenshot(mc, "hidden-mobs-search");
            search.setValue("");
        });
        then(10, () -> {
            Screen screen = Screens.current(mc);
            click(mc, findText(screen, "Minecraft"));
        });
        then(10, () -> {
            Screen screen = Screens.current(mc);
            check(screen instanceof HiddenMobsScreen, "the vanilla mob list did not open");
            click(mc, find(screen, "entity.minecraft.bat"));
            check(config.isHidden("minecraft:bat"), "clicking a mob in the list did not hide it");
            screenshot(mc, "hidden-mobs-vanilla");
            click(mc, find(screen, "heartline.options.hidden_mobs.show_mod"));
        });
        then(10, () -> {
            check(!config.isHidden("minecraft:bat"), "Show All did not show the mob again");
            Screens.open(mc, null);
        });
        then(10, () -> {
            Path file = HeartlineClient.configPath();
            check(Files.exists(file), "the config file was not written");
            check(read(file).contains("\"visibility\": \"combat\""), "the new setting was not saved: " + read(file));
        });
        String modMob = System.getProperty("heartline.modmob");
        if (modMob != null) {
            planModded(mc, modMob);
        }
    }

    private void planModded(Minecraft mc, String id) {
        HeartlineConfig config = HeartlineClient.config();
        int floor = ServerCompat.FLOOR;
        String modId = id.substring(0, id.indexOf(':'));
        LivingEntity[] mob = new LivingEntity[1];

        then(5, () -> {
            config.setVisibility(Visibility.ALWAYS);
            run(mc, "kill @e[type=minecraft:husk]");
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 15");
            run(mc, "summon " + id + " 0.5 " + floor + " 4.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
        });
        within(60, () -> find(mc, id) != null, () -> {
            mob[0] = find(mc, id);
            check(mob[0] != null, "the modded mob " + id + " did not show up on the client");
        });
        within(60, () -> HeartlineHud.drew(mob[0].getId()) && Tracker.lookId() == mob[0].getId(), () -> {
            check(HeartlineHud.drew(mob[0].getId()), "no bar above the modded mob " + id);
            check(Tracker.lookId() == mob[0].getId(), "the crosshair is not on the modded mob");
            check(HeartlineHud.panelDrawn(), "the target panel did not show the modded mob");
            screenshot(mc, "modded-bar");
            MobTypes.Mod mod = null;
            for (MobTypes.Mod each : MobTypes.byMod()) {
                if (each.id().equals(modId)) {
                    mod = each;
                }
            }
            check(mod != null, "the mod " + modId + " is not in the hidden mobs list");
            check(!mod.name().equals(modId), "the mod list shows the raw id instead of the mod's name: " + mod.name());
            boolean listed = false;
            for (MobTypes.Mob each : mod.mobs()) {
                listed |= each.id().equals(id);
            }
            check(listed, id + " is not listed under " + mod.name());
            log("modded mobs listed under " + mod.name() + ": " + mod.mobs().size());
            Screens.open(mc, new HiddenMobsScreen(null, mc.options, () -> null));
        });
        then(10, () -> {
            screenshot(mc, "modded-mods");
            String name = null;
            for (MobTypes.Mod each : MobTypes.byMod()) {
                if (each.id().equals(modId)) {
                    name = each.name();
                }
            }
            click(mc, findText(Screens.current(mc), name));
        });
        then(10, () -> {
            String key = "entity." + id.replace(':', '.');
            click(mc, find(Screens.current(mc), key));
            check(config.isHidden(id), "clicking the modded mob did not hide it");
            screenshot(mc, "modded-mobs");
            Screens.open(mc, null);
        });
        then(5, () -> {
            check(!HeartlineHud.drew(mob[0].getId()), "the hidden modded mob still got a bar");
            screenshot(mc, "modded-hidden");
            config.setHidden(id, false);
        });
    }

    private static void attack(Minecraft mc, Entity target) {
        LocalPlayer player = mc.player;
        mc.gameMode.attack(player, target);
    }

    private static LivingEntity find(Minecraft mc, String type) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && Entities.type(entity).equals(type)) {
                return living;
            }
        }
        return null;
    }

    private static Popup popupFor(Entity entity) {
        Popup last = null;
        for (Popup popup : Tracker.popups().all()) {
            if (popup.entityId() == entity.getId() && popup.kind() != Popup.Kind.HEAL) {
                last = popup;
            }
        }
        return last;
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> ServerCompat.runCommand(server, command));
    }

    private void screenshot(Minecraft mc, String name) {
        log("screenshot: " + name);
        shots.add(name);
        ServerCompat.screenshot(mc, Screens.renderTarget(mc));
    }

    private void rememberShots(Minecraft mc) {
        File[] files = new File(mc.gameDirectory, "screenshots").listFiles();
        if (files != null) {
            for (File file : files) {
                existingShots.add(file.getName());
            }
        }
    }

    private void nameShots(Minecraft mc) {
        File dir = new File(mc.gameDirectory, "screenshots");
        File[] files = dir.listFiles((parent, name) -> name.endsWith(".png") && !existingShots.contains(name));
        if (files == null) {
            return;
        }
        List<File> fresh = new ArrayList<>(List.of(files));
        fresh.sort(Comparator.comparingLong(File::lastModified).thenComparing(File::getName));
        for (int i = 0; i < fresh.size() && i < shots.size(); i++) {
            File target = new File(dir, String.format(Locale.ROOT, "hl-%02d-%s.png", i, shots.get(i)));
            if ((!target.exists() || target.delete()) && fresh.get(i).renameTo(target)) {
                log("saved " + target.getName());
            }
        }
    }

    private static void deleteWorld(Minecraft mc, String name) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(name);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void click(Minecraft mc, AbstractWidget widget) {
        Ui.click(Screens.current(mc), widget);
    }

    private static void scroll(Screen screen, boolean bottom) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSelectionList<?> list) {
                list.setScrollAmount(bottom ? Double.MAX_VALUE : 0.0);
            }
        }
    }

    private static AbstractWidget find(GuiEventListener node, String key) {
        if (node instanceof AbstractWidget widget && labelled(widget, key)) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, key);
                if (found != null) {
                    return found;
                }
            }
        }
        if (node instanceof Screen) {
            throw new AssertionError("no widget " + key + " on " + node);
        }
        return null;
    }

    private static AbstractWidget findText(GuiEventListener node, String text) {
        return findText(node, text, true);
    }

    private static AbstractWidget findText(GuiEventListener node, String text, boolean required) {
        if (node instanceof AbstractWidget widget && !(node instanceof EditBox)
                && widget.getMessage().getString().equals(text)) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = findText(child, text, false);
                if (found != null) {
                    return found;
                }
            }
        }
        if (required && node instanceof Screen) {
            throw new AssertionError("no widget reading " + text + " on " + node);
        }
        return null;
    }

    private static EditBox findBox(GuiEventListener node) {
        if (node instanceof EditBox box) {
            return box;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                EditBox found = findBox(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean labelled(AbstractWidget widget, String key) {
        String own = TextKeys.key(widget.getMessage());
        if (own == null) {
            return false;
        }
        if (own.equals(key)) {
            return true;
        }
        Object[] args = TextKeys.args(widget.getMessage());
        return args.length > 0
                && args[0] instanceof net.minecraft.network.chat.Component name
                && key.equals(TextKeys.key(name));
    }

    private static int[] uuidInts(java.util.UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void log(String message) {
        System.out.println("[heartline-selftest] " + message);
    }
}
