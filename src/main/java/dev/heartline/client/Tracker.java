package dev.heartline.client;

import dev.heartline.config.HeartlineConfig;
import dev.heartline.core.Category;
import dev.heartline.core.Popup;
import dev.heartline.core.Popups;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Tracker {
    public static final int ATTACK_WINDOW = 10;
    public static final int TARGET_TICKS = 60;
    public static final int NOTICE_TICKS = 50;
    public static final int PET_WARNING_TICKS = 80;

    private static final Map<Integer, Tracked> TRACKED = new HashMap<>();
    private static final Popups POPUPS = new Popups();

    private static int tick;
    private static int attackTick = Integer.MIN_VALUE / 2;
    private static int attackTarget = -1;
    private static boolean attackCrit;
    private static int lookId = -1;
    private static int targetId = -1;
    private static int targetUntil;
    private static String notice;
    private static int noticeUntil;
    private static String petWarning;
    private static int petWarningUntil;

    private Tracker() {
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        tick++;
        handleKeys(mc);
        POPUPS.tick();

        HeartlineConfig config = HeartlineClient.config();
        Player player = mc.player;
        double reach = config.range() + 8.0;
        double reachSq = reach * reach;
        Vec3 eye = player.getEyePosition(1.0F);
        Set<Integer> seen = new HashSet<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == player || Entities.skip(living)) {
                continue;
            }
            if (entity.distanceToSqr(player) > reachSq) {
                continue;
            }
            int id = entity.getId();
            seen.add(id);
            Tracked tracked = TRACKED.get(id);
            float health = living.getHealth();
            float max = living.getMaxHealth();
            if (tracked == null || tracked.entity != living) {
                tracked = new Tracked(living);
                TRACKED.put(id, tracked);
            } else {
                observe(config, tracked, living, health);
            }
            tracked.health = health;
            tracked.max = max;
            tracked.bar.tick(health, max);
            if (!config.lineOfSight()) {
                tracked.visible = true;
            } else if ((tick + id) % 4 == 0) {
                tracked.visible = Entities.canSee(mc.level, eye, living, player);
            }
            watchPet(config, tracked, living, player);
        }
        TRACKED.keySet().retainAll(seen);

        Entity looked = mc.crosshairPickEntity;
        lookId = looked instanceof LivingEntity && TRACKED.containsKey(looked.getId()) ? looked.getId() : -1;
        if (lookId >= 0) {
            targetId = lookId;
            targetUntil = tick + TARGET_TICKS;
        }
        if (!TRACKED.containsKey(targetId) || tick > targetUntil) {
            targetId = -1;
        }
    }

    private static void observe(HeartlineConfig config, Tracked tracked, LivingEntity living, float health) {
        float delta = tracked.health - health;
        if (Math.abs(delta) < 0.01F) {
            return;
        }
        int id = living.getId();
        boolean damage = delta > 0.0F;
        Popup.Kind kind = Popup.Kind.HEAL;
        if (damage) {
            tracked.lastHurt = tick;
            boolean mine = attackTarget == id && tick - attackTick <= ATTACK_WINDOW;
            kind = mine && attackCrit ? Popup.Kind.CRIT : Popup.Kind.DAMAGE;
            if (mine) {
                attackTarget = -1;
                targetId = id;
                targetUntil = tick + TARGET_TICKS;
            }
        } else if (health <= 0.0F || tracked.health <= 0.0F) {
            return;
        }
        if (damage ? config.damageNumbers() : config.healNumbers()) {
            POPUPS.add(id, living.getX(), living.getY() + living.getBbHeight() * 0.75, living.getZ(), kind,
                    Math.abs(delta), config.combo());
        }
    }

    private static void watchPet(HeartlineConfig config, Tracked tracked, LivingEntity living, Player player) {
        if (!config.petAlert() || !Owners.ownedBy(living, player) || tracked.max <= 0.0F) {
            return;
        }
        float fraction = tracked.health / tracked.max;
        float threshold = config.petAlertPercent() / 100.0F;
        if (!tracked.lowAlerted && tracked.health > 0.0F && fraction <= threshold) {
            tracked.lowAlerted = true;
            petWarning = Texts.translatable("heartline.pet_warning", Entities.name(living),
                    Math.round(fraction * 100.0F)).getString();
            petWarningUntil = tick + PET_WARNING_TICKS;
        } else if (tracked.lowAlerted && fraction > threshold + 0.05F) {
            tracked.lowAlerted = false;
        }
    }

    private static void handleKeys(Minecraft mc) {
        KeyMapping toggle = HeartlineClient.toggleKey();
        while (toggle != null && toggle.consumeClick()) {
            HeartlineConfig config = HeartlineClient.config();
            config.setEnabled(!config.enabled());
            HeartlineClient.saveConfig();
            notice(Texts.translatable(config.enabled() ? "heartline.notice.on" : "heartline.notice.off").getString());
        }
        KeyMapping hide = HeartlineClient.hideKey();
        while (hide != null && hide.consumeClick()) {
            toggleHidden(mc.crosshairPickEntity);
        }
    }

    public static boolean toggleHidden(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            notice(Texts.translatable("heartline.notice.look").getString());
            return false;
        }
        boolean hidden = HeartlineClient.config().toggleHidden(Entities.type(entity));
        HeartlineClient.saveConfig();
        notice(Texts.translatable(hidden ? "heartline.notice.hidden" : "heartline.notice.shown",
                Entities.typeName(entity)).getString());
        return hidden;
    }

    public static void onAttack(Player player, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || target == null) {
            return;
        }
        attackTick = tick;
        attackTarget = target.getId();
        attackCrit = Entities.canCrit(player, target);
        if (target instanceof LivingEntity) {
            targetId = target.getId();
            targetUntil = tick + TARGET_TICKS;
        }
    }

    public static void notice(String text) {
        notice = text;
        noticeUntil = tick + NOTICE_TICKS;
    }

    public static void clear() {
        TRACKED.clear();
        POPUPS.clear();
        lookId = -1;
        targetId = -1;
        petWarning = null;
    }

    public static Collection<Tracked> tracked() {
        return TRACKED.values();
    }

    public static Tracked get(int id) {
        return TRACKED.get(id);
    }

    public static Popups popups() {
        return POPUPS;
    }

    public static int now() {
        return tick;
    }

    public static int lookId() {
        return lookId;
    }

    public static Tracked target() {
        return targetId < 0 ? null : TRACKED.get(targetId);
    }

    public static String notice() {
        return tick < noticeUntil ? notice : null;
    }

    public static float noticeAlpha(float partial) {
        return Math.max(0.0F, Math.min(1.0F, (noticeUntil - tick - partial) / 10.0F));
    }

    public static String petWarning() {
        return tick < petWarningUntil ? petWarning : null;
    }

    public static float petWarningAlpha(float partial) {
        return Math.max(0.0F, Math.min(1.0F, (petWarningUntil - tick - partial) / 10.0F));
    }

    public static boolean isPet(LivingEntity entity, Player player) {
        return Entities.category(entity, player) == Category.PET;
    }
}
