package dev.heartline.showcase;

import com.mojang.datafixers.util.Pair;
import dev.heartline.client.Entities;
import dev.heartline.client.HeartlineClient;
import dev.heartline.client.gui.HeartlineSettingsScreen;
import dev.heartline.config.HeartlineConfig;
import dev.heartline.core.BarStyle;
import dev.heartline.core.ColorMode;
import dev.heartline.core.PanelPosition;
import dev.heartline.core.Visibility;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class Showcase {
    private static final String WORLD = "heartline-showcase";
    private static final long SETTLE_NANOS = 3_000_000_000L;
    private static final int GLIDE = 24;
    private static final int COMBAT = 20;
    private static final int KILL = 10;
    private static final int GROUP = 0;
    private static final int PETS = -12;
    private static final int LINEUP = 5;
    private static final int ROW = -8;
    private static final int STAND = -13;
    private static final int EDGE = 28;
    private static final String[] ARROW = {
            "B",
            "BB",
            "BWB",
            "BWWB",
            "BWWWB",
            "BWWWWB",
            "BWWWWWB",
            "BWWWWWWB",
            "BWWWWWWWB",
            "BWWWWWWWWB",
            "BWWWWWWWWWB",
            "BWWWWWWBBBBB",
            "BWWWBWWB",
            "BWWB BWWB",
            "BWB  BWWB",
            "BB    BWWB",
            "B     BWWB",
            "       BWWB",
            "        BB"};
    private static Showcase instance;

    private final Path out;
    private final Recorder recorder;
    private final long seed;
    private final List<Action> actions = new ArrayList<>();
    private final AtomicReference<BlockPos> site = new AtomicReference<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int frame;
    private long actionStart;
    private Path pendingStill;
    private boolean cursorShown;
    private boolean showHand;
    private int critAt;
    private double cursorX;
    private double cursorY;

    private interface Action {
        boolean run(int frame);
    }

    private record Key(int move, int hold, Vec3 target) {
    }

    private record Stop(int frame, Supplier<double[]> where) {
    }

    private Showcase(Path out, String ffmpeg, long seed) {
        this.out = out;
        this.recorder = new Recorder(ffmpeg);
        this.seed = seed;
    }

    public static void start() {
        String ffmpeg = System.getProperty("heartline.ffmpeg", "ffmpeg");
        long seed = Long.parseLong(System.getProperty("heartline.seed", "12345"));
        instance = new Showcase(Path.of(System.getProperty("heartline.showcase")), ffmpeg, seed);
    }

    public static boolean hideHand() {
        return instance != null && instance.started && !instance.showHand;
    }

    public static void frame() {
        if (instance != null) {
            instance.onFrame();
        }
    }

    public static void drawCursor(GuiGraphicsExtractor graphics) {
        if (instance == null || !instance.started || !instance.cursorShown) {
            return;
        }
        float cell = 2.0F / (float) Minecraft.getInstance().getWindow().getGuiScale();
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) instance.cursorX, (float) instance.cursorY);
        graphics.pose().scale(cell, cell);
        for (int row = 0; row < ARROW.length; row++) {
            String line = ARROW[row];
            int x = 0;
            while (x < line.length()) {
                char c = line.charAt(x);
                int end = x;
                while (end < line.length() && line.charAt(end) == c) {
                    end++;
                }
                if (c != ' ') {
                    graphics.fill(x, row, end, row + 1, c == 'B' ? 0xFF000000 : 0xFFFFFFFF);
                }
                x = end;
            }
        }
        graphics.pose().popMatrix();
    }

    private void onFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 120) {
                started = true;
                setUpOptions(mc);
                plan(mc);
                log("creating world with seed " + seed);
                deleteWorld(mc);
                Worlds.create(mc, WORLD, false, seed);
            }
            return;
        }
        Clock.step();
        mc.gui.toastManager().clear();
        try {
            while (index < actions.size()) {
                if (frame == 0) {
                    actionStart = System.nanoTime();
                }
                if (!actions.get(index).run(frame++)) {
                    if (cursorShown && Screens.current(mc) != null) {
                        double scale = mc.getWindow().getGuiScale();
                        Ui.moveMouse(mc, cursorX * scale, cursorY * scale);
                    }
                    return;
                }
                index++;
                frame = 0;
            }
        } catch (Throwable e) {
            log("FAILED: " + e);
            e.printStackTrace();
        }
        finish(mc);
    }

    private void finish(Minecraft mc) {
        finished = true;
        Clock.release();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(90_000L);
            } catch (InterruptedException e) {
                return;
            }
            log("the game did not close, forcing it");
            Runtime.getRuntime().halt(1);
        }, "heartline-showcase-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        HeartlineClient.config().resetToDefaults();
        HeartlineClient.saveConfig();
        log("showcase done");
        mc.stop();
    }

    private static void setUpOptions(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.renderDistance().set(12);
        mc.options.bobView().set(false);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.guiScale().set(4);
        mc.resizeGui();
    }

    private void once(Runnable action) {
        actions.add(f -> {
            action.run();
            return true;
        });
    }

    private void until(BooleanSupplier ready) {
        actions.add(f -> ready.getAsBoolean());
    }

    private void holdFor(int frames, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= frames;
        });
    }

    private void settle(Minecraft mc, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= 90 && System.nanoTime() - actionStart > SETTLE_NANOS
                    && (mc.levelRenderer.hasRenderedAllSections() || System.nanoTime() - actionStart > 20 * SETTLE_NANOS);
        });
    }

    private void record(String name, int frames, IntConsumer script) {
        actions.add(f -> {
            if (f == 0) {
                log("recording " + name);
                recorder.start(out.resolve("clips").resolve(name + ".mp4"));
            } else {
                Path still = pendingStill;
                pendingStill = null;
                recorder.capture(still);
            }
            if (f < frames) {
                script.accept(f);
                return false;
            }
            return true;
        });
        until(recorder::drained);
        once(recorder::stop);
    }

    private void shot(String name) {
        pendingStill = out.resolve("stills").resolve(name + ".png");
    }

    static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void cmd(Minecraft mc, String format, Object... args) {
        run(mc, String.format(Locale.ROOT, format, args));
    }

    private static void face(Minecraft mc, double yaw, double pitch) {
        LocalPlayer player = mc.player;
        player.setYRot((float) yaw);
        player.setXRot((float) pitch);
        player.yRotO = (float) yaw;
        player.xRotO = (float) pitch;
        player.setYHeadRot((float) yaw);
        player.yHeadRotO = (float) yaw;
    }

    private static void lookAt(Minecraft mc, Vec3 target) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        face(mc, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static void follow(Minecraft mc, int f, Key... keys) {
        Vec3 target = keys[0].target();
        int t = f - keys[0].hold();
        for (int i = 1; i < keys.length && t >= 0; i++) {
            Key key = keys[i];
            if (t < key.move()) {
                target = lerp(keys[i - 1].target(), key.target(), smooth(t / (double) key.move()));
                break;
            }
            t -= key.move();
            target = key.target();
            t -= key.hold();
        }
        lookAt(mc, target);
    }

    private void cursor(int f, Stop... stops) {
        double[] at = stops[0].where().get();
        for (int i = 1; i < stops.length; i++) {
            Stop stop = stops[i];
            if (f >= stop.frame()) {
                at = stop.where().get();
            } else if (f >= stop.frame() - GLIDE) {
                double[] to = stop.where().get();
                double s = smooth((f - (stop.frame() - GLIDE)) / (double) GLIDE);
                at = new double[] {lerp(at[0], to[0], s), lerp(at[1], to[1], s)};
                break;
            } else {
                break;
            }
        }
        cursorShown = true;
        cursorX = at[0];
        cursorY = at[1];
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private static boolean wanted(String scene) {
        String only = System.getProperty("heartline.scenes");
        return only == null || List.of(only.split(",")).contains(scene);
    }

    private Vec3 at(double dx, double dy, double dz) {
        BlockPos c = site.get();
        return new Vec3(c.getX() + dx + 0.5, c.getY() + dy, c.getZ() + dz + 0.5);
    }

    private static int height(ServerLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
    }

    private void locate(Minecraft mc) {
        once(() -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                ChunkGenerator generator = level.getChunkSource().getGenerator();
                RandomState noise = level.getChunkSource().randomState();
                Pair<BlockPos, Holder<Biome>> pair = level.findClosestBiome3d(h -> h.is(Biomes.PLAINS),
                        BlockPos.ZERO, 6400, 32, 64);
                BlockPos found = pair == null ? BlockPos.ZERO : pair.getFirst();
                BlockPos best = null;
                int bestScore = Integer.MAX_VALUE;
                for (int ox = -1536; ox <= 1536; ox += 96) {
                    for (int oz = -1536; oz <= 1536; oz += 96) {
                        int cx = found.getX() + ox;
                        int cz = found.getZ() + oz;
                        int centre = generator.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, level, noise);
                        if (!level.getBiome(new BlockPos(cx, centre, cz)).is(Biomes.PLAINS)) {
                            continue;
                        }
                        List<Integer> heights = new ArrayList<>();
                        int wet = 0;
                        for (int dx = -40; dx <= 40; dx += 8) {
                            for (int dz = -40; dz <= 40; dz += 8) {
                                int top = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.WORLD_SURFACE_WG,
                                        level, noise);
                                int floor = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.OCEAN_FLOOR_WG,
                                        level, noise);
                                heights.add(top);
                                if (top != floor) {
                                    wet++;
                                }
                            }
                        }
                        int usual = mode(heights);
                        int score = wet * 3;
                        for (int h : heights) {
                            if (Math.abs(h - usual) > 1) {
                                score++;
                            }
                        }
                        if (score < bestScore) {
                            bestScore = score;
                            best = new BlockPos(cx, centre, cz);
                        }
                    }
                }
                site.set(best == null ? found : best);
                log("plains at " + site.get().toShortString() + ", " + bestScore + " uneven samples");
            });
        });
        until(() -> site.get() != null);
    }

    private static int mode(List<Integer> values) {
        Map<Integer, Integer> counts = new HashMap<>();
        int best = values.get(0);
        for (int value : values) {
            int count = counts.merge(value, 1, Integer::sum);
            if (count > counts.get(best)) {
                best = value;
            }
        }
        return best;
    }

    private void level(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        int ground = server.submit(() -> {
            ServerLevel level = server.overworld();
            BlockPos c = site.get();
            List<Integer> heights = new ArrayList<>();
            for (int dx = -EDGE; dx <= EDGE; dx += 2) {
                for (int dz = -EDGE; dz <= EDGE; dz += 2) {
                    heights.add(height(level, c.getX() + dx, c.getZ() + dz));
                }
            }
            return mode(heights);
        }).join();
        BlockPos c = site.get();
        site.set(new BlockPos(c.getX(), ground + 1, c.getZ()));
        log("set stands at y " + (ground + 1));
    }

    private static void fill(Minecraft mc, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        int layers = Math.max(1, 32768 / ((Math.abs(x2 - x1) + 1) * (Math.abs(z2 - z1) + 1)));
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y += layers) {
            cmd(mc, "fill %d %d %d %d %d %d %s", x1, y, z1, x2, Math.min(y + layers - 1, Math.max(y1, y2)), z2, block);
        }
    }

    private void summon(Minecraft mc, String type, Vec3 at, String extra) {
        cmd(mc, "summon minecraft:%s %.2f %.2f %.2f {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"scene\"],"
                + "Rotation:[180f,0f]%s}", type, at.x, at.y, at.z, extra);
    }

    private static String owner(Minecraft mc) {
        int[] id = UUIDUtil.uuidToIntArray(mc.player.getUUID());
        return String.format(Locale.ROOT, ",Owner:[I;%d,%d,%d,%d]", id[0], id[1], id[2], id[3]);
    }

    private static String helmet(String extra) {
        return ",equipment:{head:{id:\"minecraft:iron_helmet\"}" + extra + "}";
    }

    private static String health(float value, float max) {
        return String.format(Locale.ROOT, ",Health:%.1ff,attributes:[{id:\"minecraft:max_health\",base:%.1fd}]", value,
                max);
    }

    private void buildSet(Minecraft mc) {
        BlockPos c = site.get();
        int x = c.getX();
        int z = c.getZ();
        int ground = c.getY() - 1;
        fill(mc, x - EDGE, ground + 1, z - EDGE, x + EDGE, ground + 40, z + EDGE, "minecraft:air");
        fill(mc, x - EDGE, ground - 6, z - EDGE, x + EDGE, ground - 1, z + EDGE, "minecraft:dirt");
        fill(mc, x - EDGE, ground, z - EDGE, x + EDGE, ground, z + EDGE, "minecraft:grass_block");
        cmd(mc, "place feature minecraft:fancy_oak %d %d %d", x, ground + 1, z + 15);
        String[] kinds = {"oak", "birch", "fancy_oak"};
        int[][] trees = {{-10, 13}, {9, 18}, {-17, 20}, {16, 11}, {-23, 9}, {23, 7}, {-5, 25}, {19, 24}, {25, 17},
                {-25, 19}, {-20, -24}, {8, -25}, {22, -23}, {-6, -26}};
        for (int i = 0; i < trees.length; i++) {
            cmd(mc, "place feature minecraft:%s %d %d %d", kinds[i % kinds.length], x + trees[i][0], ground + 1,
                    z + trees[i][1]);
        }
        Random random = new Random(seed);
        String[] flowers = {"dandelion", "poppy", "oxeye_daisy", "cornflower", "azure_bluet"};
        for (int dx = -EDGE + 1; dx <= EDGE - 1; dx++) {
            for (int dz = -EDGE + 1; dz <= EDGE - 1; dz++) {
                if (dz >= STAND - 2 && dz <= ROW + 1 || Math.abs(dx) <= 9 && dz >= -1 && dz <= LINEUP + 2) {
                    continue;
                }
                double roll = random.nextDouble();
                String block;
                if (roll < 0.12) {
                    block = "short_grass";
                } else if (roll < 0.145) {
                    block = flowers[random.nextInt(flowers.length)];
                } else {
                    continue;
                }
                cmd(mc, "setblock %d %d %d minecraft:%s keep", x + dx, ground + 1, z + dz, block);
            }
        }
        run(mc, "kill @e[type=!minecraft:player]");
    }

    private void lineup(Minecraft mc) {
        String owner = owner(mc);
        summon(mc, "wolf", at(-4.5, 0, LINEUP), owner + ",CollarColor:14b" + health(30, 40));
        summon(mc, "sheep", at(-2.7, 0, LINEUP), ",Color:0b" + health(3, 8));
        summon(mc, "cow", at(-0.9, 0, LINEUP), "");
        summon(mc, "creeper", at(0.9, 0, LINEUP), health(13, 20));
        summon(mc, "zombie", at(2.7, 0, LINEUP), helmet(""));
        summon(mc, "skeleton", at(4.5, 0, LINEUP), health(9, 20) + helmet(",mainhand:{id:\"minecraft:bow\"}"));
    }

    private void group(Minecraft mc) {
        summon(mc, "zombie", at(GROUP - 3, 0, ROW), health(13, 20) + helmet(""));
        summon(mc, "wolf", at(GROUP - 1, 0, ROW), health(5, 8));
        summon(mc, "cow", at(GROUP + 1, 0, ROW), health(6, 10));
        summon(mc, "cat", at(GROUP + 3, 0, ROW), owner(mc) + ",CollarColor:11b" + health(7, 10));
    }

    private static void kit(Minecraft mc, String... items) {
        run(mc, "clear @p");
        for (int i = 0; i < items.length; i++) {
            cmd(mc, "item replace entity @p hotbar.%d with minecraft:%s", i, items[i]);
        }
    }

    private static LivingEntity mob(Minecraft mc, String type) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && Entities.type(entity).equals("minecraft:" + type)) {
                return living;
            }
        }
        throw new IllegalStateException("no " + type + " in the world");
    }

    private static void attack(Minecraft mc, Entity target) {
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
    }

    private void scene(Minecraft mc, HeartlineConfig config, Vec3 feet, Runnable setup, Runnable hold) {
        once(() -> {
            Screens.open(mc, null);
            config.resetToDefaults();
            config.clearHidden();
            cursorShown = false;
            showHand = false;
            critAt = -1;
            run(mc, "gamemode creative @p");
            cmd(mc, "tp @p %.3f %.3f %.3f", feet.x, feet.y, feet.z);
            run(mc, "kill @e[type=!minecraft:player]");
            kit(mc, "diamond_sword", "bow", "bread 16");
            select(mc, 8);
        });
        holdFor(80, hold);
        once(() -> run(mc, "kill @e[type=minecraft:item]"));
        holdFor(20, hold);
        once(setup);
        settle(mc, hold);
    }

    private void plan(Minecraft mc) {
        HeartlineConfig config = HeartlineClient.config();

        until(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null);
        once(() -> {
            log("world loaded");
            Clock.fix();
            config.resetToDefaults();
            run(mc, "time set 2500");
            run(mc, "weather clear 1000000");
            run(mc, "gamerule advance_time false");
            run(mc, "gamerule spawn_mobs false");
            run(mc, "gamerule random_tick_speed 0");
            run(mc, "difficulty easy");
        });
        locate(mc);
        once(() -> {
            BlockPos c = site.get();
            cmd(mc, "forceload add %d %d %d %d", c.getX() - 40, c.getZ() - 40, c.getX() + 40, c.getZ() + 40);
            cmd(mc, "tp @p %d %d %d", c.getX(), c.getY() + 45, c.getZ());
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        });
        settle(mc, () -> {
        });
        once(() -> level(mc));
        once(() -> buildSet(mc));
        holdFor(60, () -> {
        });
        once(() -> {
            mc.player.getAbilities().flying = false;
            mc.player.onUpdateAbilities();
        });
        once(() -> film(mc, config));
    }

    private void film(Minecraft mc, HeartlineConfig config) {
        Vec3 spot = at(0, 0, -2);

        if (wanted("intro")) {
            Key left = new Key(0, 30, at(1.5, 1.6, LINEUP));
            Key right = new Key(270, 0, at(-1.5, 1.6, LINEUP));
            scene(mc, config, spot, () -> {
                config.setVisibility(Visibility.ALWAYS);
                lineup(mc);
            }, () -> follow(mc, 0, left));
            record("01-intro", 330, f -> {
                follow(mc, f, left, right);
                if (f == 200) {
                    shot("intro");
                }
            });
        }

        if (wanted("combat")) {
            Vec3 stand = at(COMBAT, 0, ROW - 2.9);
            Vec3 chest = at(COMBAT, 1.2, ROW);
            scene(mc, config, stand, () -> {
                summon(mc, "zombie", at(COMBAT, 0, ROW), health(40, 40) + helmet(""));
                select(mc, 0);
                showHand = true;
            }, () -> lookAt(mc, chest));
            record("02-combat", 330, f -> {
                if (mc.player.onGround()) {
                    lookAt(mc, chest);
                }
                LivingEntity zombie = mob(mc, "zombie");
                if (f == 30 || f == 75 || f == 120) {
                    attack(mc, zombie);
                } else if (f == 215 && mc.player.onGround()) {
                    mc.player.setDeltaMovement(0.0, 0.42, 0.0);
                } else if (f > 220 && critAt < 0 && !mc.player.onGround() && mc.player.fallDistance > 0.0
                        && mc.player.getY() - stand.y < 0.6) {
                    critAt = f;
                    attack(mc, zombie);
                }
                if (f == 135) {
                    shot("combo");
                } else if (critAt >= 0 && f == critAt + 12) {
                    shot("crit");
                }
            });
        }

        if (wanted("kill")) {
            Vec3 stand = at(KILL, 0, ROW - 2.9);
            Vec3 chest = at(KILL, 1.2, ROW);
            scene(mc, config, stand, () -> {
                kit(mc, "wooden_sword", "iron_sword", "netherite_axe");
                summon(mc, "vindicator", at(KILL, 0, ROW), health(10, 24)
                        + ",equipment:{mainhand:{id:\"minecraft:iron_axe\"}}");
                select(mc, 0);
                showHand = true;
            }, () -> lookAt(mc, chest));
            record("03-kill", 330, f -> {
                lookAt(mc, chest);
                if (f == 100) {
                    select(mc, 1);
                } else if (f == 190) {
                    select(mc, 2);
                } else if (f == 265) {
                    attack(mc, mob(mc, "vindicator"));
                }
                if (f == 80) {
                    shot("hits-wooden");
                } else if (f == 170) {
                    shot("hits-iron");
                } else if (f == 250) {
                    shot("one-more-hit");
                } else if (f == 272) {
                    shot("kill");
                }
            });
        }

        Vec3 group = at(GROUP, 0, ROW - 3.8);
        Vec3 middle = at(GROUP, 0.9, ROW);

        if (wanted("styles")) {
            BarStyle[] styles = {BarStyle.SEGMENTED, BarStyle.FLAT, BarStyle.SLIM, BarStyle.PIP, BarStyle.OUTLINE,
                    BarStyle.GLASS, BarStyle.NEON, BarStyle.CAPSULE, BarStyle.GAUGE, BarStyle.BRACKET};
            scene(mc, config, group, () -> group(mc), () -> lookAt(mc, middle));
            record("04-styles", 55 * (styles.length + 1), f -> {
                lookAt(mc, middle);
                int step = f / 55;
                config.setStyle(styles[step % styles.length]);
                config.setColors(step >= styles.length ? ColorMode.CATEGORY : ColorMode.HEALTH);
                if (f % 55 == 40) {
                    shot(step >= styles.length ? "colors-by-type" : "style-" + styles[step].id());
                }
            });
        }

        if (wanted("crosshair")) {
            Key zombie = new Key(0, 50, at(GROUP - 3, 1.2, ROW));
            Key wolf = new Key(22, 50, at(GROUP - 1, 0.5, ROW));
            Key cow = new Key(22, 50, at(GROUP + 1, 0.8, ROW));
            Key gap = new Key(22, 70, at(GROUP + 2, 0.9, ROW));
            scene(mc, config, at(GROUP, 0, ROW - 3), () -> {
                group(mc);
                config.setVisibility(Visibility.LOOKING);
                config.setPanel(PanelPosition.OFF);
            }, () -> follow(mc, 0, zombie));
            record("05-crosshair", 300, f -> {
                follow(mc, f, zombie, wolf, cow, gap);
                if (f == 40) {
                    shot("crosshair-only");
                }
            });
        }

        if (wanted("pets")) {
            Vec3 stand = at(PETS, 0, ROW - 2.8);
            Vec3 wolf = at(PETS, 0.5, ROW);
            scene(mc, config, stand, () -> summon(mc, "wolf", at(PETS, 0, ROW), owner(mc) + ",CollarColor:14b"
                    + health(40, 40)), () -> lookAt(mc, wolf));
            record("06-pets", 320, f -> {
                lookAt(mc, wolf);
                if (f == 30 || f == 80) {
                    run(mc, "damage @e[type=minecraft:wolf,limit=1] 12 minecraft:generic");
                } else if (f == 130) {
                    run(mc, "damage @e[type=minecraft:wolf,limit=1] 6 minecraft:generic");
                } else if (f == 225) {
                    run(mc, "effect give @e[type=minecraft:wolf] minecraft:instant_health 1 1 true");
                }
                if (f == 175) {
                    shot("pet-warning");
                } else if (f == 240) {
                    shot("heal");
                }
            });
        }

        if (wanted("placement")) {
            Vec3 zombie = at(GROUP - 3, 1.2, ROW);
            PanelPosition[] spots = {PanelPosition.TOP_LEFT, PanelPosition.TOP_CENTER, PanelPosition.TOP_RIGHT,
                    PanelPosition.CROSSHAIR, PanelPosition.TOP_LEFT};
            scene(mc, config, at(GROUP - 3, 0, ROW - 3.2), () -> {
                group(mc);
                select(mc, 0);
            }, () -> lookAt(mc, zombie));
            record("07-placement", 300, f -> {
                lookAt(mc, zombie);
                config.setPanel(spots[Math.min(spots.length - 1, f / 60)]);
                if (f == 40) {
                    shot("panel-top-left");
                } else if (f == 160) {
                    shot("panel-top-right");
                } else if (f == 220) {
                    shot("panel-crosshair");
                }
            });
        }

        if (wanted("settings")) {
            scene(mc, config, group, () -> group(mc), () -> lookAt(mc, middle));
            once(() -> Screens.open(mc, new HeartlineSettingsScreen(null, mc.options)));
            holdFor(20, () -> cursor(0, new Stop(0, () -> corner(mc))));
            record("08-settings", 330, f -> {
                if (f < 235) {
                    cursor(f, new Stop(0, () -> corner(mc)),
                            new Stop(40, () -> labelAt(mc, "Style", 0.5)),
                            new Stop(115, () -> labelAt(mc, "Colors", 0.5)),
                            new Stop(165, () -> sliderAt(mc, "Bar Size", 2.0 / 3.0)),
                            new Stop(210, () -> corner(mc)));
                }
                if (f == 50 || f == 80) {
                    clickLabel(mc, "Style", 0.5);
                } else if (f == 125) {
                    clickLabel(mc, "Colors", 0.5);
                } else if (f == 175) {
                    clickSlider(mc, "Bar Size", 2.0 / 3.0);
                } else if (f == 215) {
                    shot("settings");
                } else if (f == 235) {
                    cursorShown = false;
                    Screens.open(mc, null);
                }
                lookAt(mc, middle);
                if (f == 300) {
                    shot("settings-result");
                }
            });
            once(() -> Screens.open(mc, null));
        }

        if (wanted("outro")) {
            once(() -> run(mc, "time set 12300"));
            Key from = new Key(0, 0, at(-1.5, 1.6, LINEUP));
            Key to = new Key(330, 0, at(1.5, 1.6, LINEUP));
            scene(mc, config, spot, () -> {
                config.setVisibility(Visibility.ALWAYS);
                lineup(mc);
            }, () -> follow(mc, 0, from, to));
            record("09-outro", 330, f -> {
                follow(mc, f, from, to);
                if (f == 150) {
                    shot("sunset");
                }
            });
            once(() -> run(mc, "time set 2500"));
        }
    }

    private static double[] corner(Minecraft mc) {
        Screen screen = Screens.current(mc);
        return new double[] {screen.width - 36, screen.height - 30};
    }

    private static double[] labelAt(Minecraft mc, String label, double along) {
        AbstractWidget widget = find(Screens.current(mc), label);
        if (widget == null) {
            throw new IllegalStateException("no widget " + label);
        }
        return new double[] {widget.getX() + widget.getWidth() * along, widget.getY() + widget.getHeight() / 2.0};
    }

    private static void clickLabel(Minecraft mc, String label, double along) {
        double[] at = labelAt(mc, label, along);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static double[] sliderAt(Minecraft mc, String label, double value) {
        AbstractWidget widget = find(Screens.current(mc), label);
        if (widget == null) {
            throw new IllegalStateException("no slider " + label);
        }
        return new double[] {widget.getX() + 4 + value * (widget.getWidth() - 8), widget.getY() + widget.getHeight() / 2.0};
    }

    private static void clickSlider(Minecraft mc, String label, double value) {
        double[] at = sliderAt(mc, label, value);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static AbstractWidget find(GuiEventListener node, String label) {
        if (node instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(label + ":")) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void deleteWorld(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static void log(String message) {
        System.out.println("[heartline-showcase] " + message);
    }
}
