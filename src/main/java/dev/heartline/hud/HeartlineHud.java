package dev.heartline.hud;

import dev.heartline.client.Entities;
import dev.heartline.client.HeartlineClient;
import dev.heartline.client.Texts;
import dev.heartline.client.Tracked;
import dev.heartline.client.Tracker;
import dev.heartline.config.HeartlineConfig;
import dev.heartline.core.BarStyle;
import dev.heartline.core.Category;
import dev.heartline.core.ColorMode;
import dev.heartline.core.HealthText;
import dev.heartline.core.Numbers;
import dev.heartline.core.Palette;
import dev.heartline.core.PanelPosition;
import dev.heartline.core.Popup;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HeartlineHud {
    public static final int BAR_WIDTH = 40;
    private static final int FINE = 4;
    private static final float TEXT = 0.5F;
    private static final float MIN_SCALE = 0.45F;
    private static final float MAX_SCALE = 1.4F;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DAMAGE = 0xFFFF6B5E;
    private static final int CRIT = 0xFFFFC13B;
    private static final int HEAL = 0xFF7CF07C;

    private static final float[] OUT = new float[2];

    private static final Set<Integer> DRAWN = new HashSet<>();

    private static int barsDrawn;
    private static int popupsDrawn;
    private static boolean panelDrawn;

    private HeartlineHud() {
    }

    private record Bar(Tracked tracked, Category category, float x, float y, float scale, float alpha, double distance) {
    }

    public static void render(Minecraft mc, Canvas canvas, float partial) {
        DRAWN.clear();
        barsDrawn = 0;
        popupsDrawn = 0;
        panelDrawn = false;
        Player player = mc.player;
        if (mc.level == null || player == null) {
            return;
        }
        HeartlineConfig config = HeartlineClient.config();
        if (config.enabled()) {
            Projection projection = Projector.create(mc, partial, canvas.width(), canvas.height());
            if (config.overhead()) {
                drawBars(canvas, config, projection, player, partial);
            }
            drawPopups(canvas, config, projection, player, partial);
            drawPanel(canvas, config, player, partial);
        }
        drawNotices(canvas, partial);
    }

    static boolean shows(HeartlineConfig config, Category category) {
        switch (category) {
            case HOSTILE:
                return config.hostile();
            case BOSS:
                return config.bosses();
            case NEUTRAL:
                return config.neutral();
            case PASSIVE:
                return config.passive();
            case PLAYER:
                return config.players();
            default:
                return true;
        }
    }

    private static void drawBars(Canvas canvas, HeartlineConfig config, Projection projection, Player player, float partial) {
        List<Bar> bars = new ArrayList<>();
        int now = Tracker.now();
        int lookId = Tracker.lookId();
        for (Tracked tracked : Tracker.tracked()) {
            LivingEntity entity = tracked.entity();
            Category category = Entities.category(entity, player);
            if (!shows(config, category) || config.isHidden(Entities.type(entity))) {
                continue;
            }
            boolean looking = entity.getId() == lookId;
            boolean pet = category == Category.PET && config.pets();
            if (!looking && !tracked.visible()) {
                continue;
            }
            Vec3 pos = entity.getPosition(partial);
            double top = pos.y + entity.getBbHeight() + 0.3 + config.height() + (Entities.nameTagShown(entity) ? 0.35 : 0.0);
            double dx = pos.x - projection.cameraX();
            double dy = top - projection.cameraY();
            double dz = pos.z - projection.cameraZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (!looking && !pet && distance > config.range()) {
                continue;
            }
            boolean damaged = tracked.health() < tracked.max() - 0.01F;
            int since = now - tracked.lastHurt();
            int fadeTicks = config.fadeSeconds() * 20;
            float alpha = 1.0F;
            if (!pet) {
                if (!config.visibility().shows(damaged, looking, since, fadeTicks)) {
                    continue;
                }
                alpha = config.visibility().fade(damaged, looking, since, fadeTicks);
                if (!looking) {
                    alpha *= clamp((float) (config.range() - distance) / 2.0F, 0.0F, 1.0F);
                }
            }
            if (entity.isDeadOrDying()) {
                alpha *= clamp(1.0F - (entity.deathTime + partial) / 20.0F, 0.0F, 1.0F);
            }
            if (alpha <= 0.02F) {
                continue;
            }
            float scale = screenScale(projection, pos.x, top, pos.z, dx, dz, 0.9F / BAR_WIDTH);
            if (scale <= 0.0F) {
                continue;
            }
            float x = OUT[0];
            float y = OUT[1];
            scale = clamp(scale, MIN_SCALE, MAX_SCALE) * (float) config.scale();
            bars.add(new Bar(tracked, category, x, y, scale, alpha, distance));
        }
        bars.sort(Comparator.comparingDouble(Bar::distance).reversed());
        for (Bar bar : bars) {
            drawBar(canvas, config, bar, partial);
            DRAWN.add(bar.tracked().entity().getId());
        }
        barsDrawn = bars.size();
    }

    private static float screenScale(Projection projection, double x, double y, double z, double dx, double dz, float perPixel) {
        double flat = Math.sqrt(dx * dx + dz * dz);
        double rx = flat < 0.001 ? 1.0 : -dz / flat;
        double rz = flat < 0.001 ? 0.0 : dx / flat;
        if (!projection.project(x + rx, y, z + rz, OUT)) {
            return -1.0F;
        }
        float ax = OUT[0];
        float ay = OUT[1];
        if (!projection.project(x, y, z, OUT)) {
            return -1.0F;
        }
        float pixelsPerBlock = (float) Math.hypot(ax - OUT[0], ay - OUT[1]);
        return pixelsPerBlock * perPixel;
    }

    private static void drawBar(Canvas canvas, HeartlineConfig config, Bar bar, float partial) {
        Tracked tracked = bar.tracked();
        LivingEntity entity = tracked.entity();
        float alpha = bar.alpha();
        canvas.push();
        canvas.translate(bar.x(), bar.y());
        canvas.scale(bar.scale());
        int height = config.style().height();
        fillBar(canvas, config, tracked, bar.category(), BAR_WIDTH, height, alpha, partial);

        if (bar.scale() * TEXT >= 0.28F) {
            canvas.push();
            canvas.scale(TEXT);
            int barTop = -(int) Math.ceil(height / TEXT);
            if (config.names()) {
                String name = Entities.name(entity);
                int color = Palette.alpha(Palette.lerp(bar.category().color(), WHITE, 0.55F), alpha);
                canvas.text(name, -canvas.textWidth(name) / 2, barTop - canvas.lineHeight() - 1, color);
            }
            String health = Numbers.health(config.healthText(), tracked.health(), tracked.max());
            int armor = config.armor() ? Entities.armor(entity) : 0;
            drawInfoRow(canvas, health, armor, 0, 3, alpha, true);
            canvas.pop();
        }
        canvas.pop();
    }

    private static void fillBar(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                                int width, int height, float alpha, float partial) {
        switch (config.style()) {
            case PIP:
                fillPips(canvas, config, tracked, category, width, height, alpha, partial);
                return;
            case OUTLINE:
                fillOutline(canvas, config, tracked, category, width, height, alpha, partial);
                return;
            case GLASS:
                StyledBars.glass(canvas, config, tracked, category, width, height, alpha, partial);
                return;
            case NEON:
                StyledBars.neon(canvas, config, tracked, category, width, height, alpha, partial);
                return;
            case CAPSULE:
                StyledBars.capsule(canvas, config, tracked, category, width, height, alpha, partial);
                return;
            case GAUGE:
                StyledBars.gauge(canvas, config, tracked, category, width, height, alpha, partial);
                return;
            case BRACKET:
                StyledBars.bracket(canvas, config, tracked, category, width, height, alpha, partial);
                return;
            default:
                fillFlat(canvas, config, tracked, category, width, height, alpha, partial);
        }
    }

    private static void fillFlat(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                                int width, int height, float alpha, float partial) {
        float max = Math.max(tracked.max(), 0.001F);
        float shown = clamp(tracked.bar().shown(partial) / max, 0.0F, 1.0F);
        float trail = clamp(tracked.bar().trail(partial) / max, 0.0F, 1.0F);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int x0 = -w / 2;
        int y0 = -h;
        int inset = Math.max(1, Math.min(FINE, h / 5));
        chamferRect(canvas, x0 - FINE, y0 - FINE, x0 + w + FINE, y0 + h + FINE, Palette.alpha(Palette.BACKGROUND, alpha), FINE, true, true);
        chamferRect(canvas, x0, y0, x0 + w, y0 + h, Palette.alpha(Palette.BORDER, alpha), FINE, true, true);
        int ix0 = x0 + inset;
        int ix1 = x0 + w - inset;
        int iy0 = y0 + inset;
        int iy1 = y0 + h - inset;
        int innerW = Math.max(1, ix1 - ix0);
        chamferRect(canvas, ix0, iy0, ix1, iy1, Palette.alpha(Palette.EMPTY, alpha), FINE, true, true);
        if (config.trail() && trail > shown) {
            int trailW = Math.round(innerW * trail);
            chamferRect(canvas, ix0, iy0, ix0 + trailW, iy1, Palette.alpha(Palette.TRAIL, alpha), FINE, true, trail >= 0.999F);
        }
        int color = config.colors() == ColorMode.HEALTH ? Palette.health(shown) : category.color();
        int filled = Math.round(innerW * shown);
        if (filled > 0) {
            boolean full = shown >= 0.999F;
            chamferRect(canvas, ix0, iy0, ix0 + filled, iy1, Palette.alpha(color, alpha), FINE, true, full);
            if (height >= 3) {
                int innerH = iy1 - iy0;
                int band = Math.max(1, innerH * 3 / 10);
                chamferRect(canvas, ix0, iy0, ix0 + filled, iy0 + band,
                        Palette.alpha(Palette.lerp(color, WHITE, 0.45F), alpha), FINE, true, full);
                chamferRect(canvas, ix0, iy1 - band, ix0 + filled, iy1,
                        Palette.alpha(Palette.darker(color, 0.35F), alpha), FINE, true, full);
            }
            float flash = tracked.bar().flash(partial);
            if (flash > 0.0F) {
                chamferRect(canvas, ix0, iy0, ix0 + filled, iy1, Palette.alpha(WHITE, flash * 0.7F * alpha), FINE, true, full);
            }
        }
        float heal = tracked.bar().heal(partial);
        if (heal > 0.0F) {
            int glow = Palette.alpha(Palette.HEAL, heal * alpha);
            canvas.fill(x0 - FINE, y0 - FINE, x0 + w + FINE, y0, glow);
            canvas.fill(x0 - FINE, y0 + h, x0 + w + FINE, y0 + h + FINE, glow);
        }
        if (config.style() == BarStyle.SEGMENTED) {
            int segments = segments(max);
            int divider = Palette.alpha(0x90000000, alpha);
            for (int i = 1; i < segments; i++) {
                int x = ix0 + Math.round((float) innerW * i / segments);
                canvas.fill(x - 1, iy0, x + 1, iy1, divider);
            }
        }
        canvas.pop();
    }

    private static void chamferRect(Canvas canvas, int x0, int y0, int x1, int y1, int color, int chamfer,
                                     boolean capLeft, boolean capRight) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) {
            return;
        }
        int c = Math.max(0, Math.min(chamfer, h / 4));
        int cl = capLeft ? Math.min(c, w / 4) : 0;
        int cr = capRight ? Math.min(c, w / 4) : 0;
        canvas.fill(x0 + cl, y0, x1 - cr, y1, color);
        if (c > 0 && (cl > 0 || cr > 0)) {
            canvas.fill(x0, y0 + c, x1, y1 - c, color);
        }
    }

    private static void fillPips(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                                int width, int height, float alpha, float partial) {
        float max = Math.max(tracked.max(), 0.001F);
        float shown = clamp(tracked.bar().shown(partial) / max, 0.0F, 1.0F);
        float trail = clamp(tracked.bar().trail(partial) / max, 0.0F, 1.0F);
        float flash = tracked.bar().flash(partial);
        float heal = tracked.bar().heal(partial);
        int color = config.colors() == ColorMode.HEALTH ? Palette.health(shown) : category.color();
        int segments = segments(max);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int gap = Math.max(1, FINE / 2);
        int pipWidth = Math.max(FINE, (w - gap * (segments - 1)) / segments);
        int totalWidth = pipWidth * segments + gap * (segments - 1);
        int x0 = -totalWidth / 2;
        int y0 = -h;
        int chamfer = Math.max(1, Math.min(FINE / 2, h / 6));
        int inset = Math.max(1, Math.min(FINE / 2, h / 6));
        for (int i = 0; i < segments; i++) {
            int px = x0 + i * (pipWidth + gap);
            float segStart = (float) i / segments;
            float segEnd = (float) (i + 1) / segments;
            float fillFrac = clamp((shown - segStart) / (segEnd - segStart), 0.0F, 1.0F);
            float trailFrac = clamp((trail - segStart) / (segEnd - segStart), 0.0F, 1.0F);
            chamferRect(canvas, px - FINE / 2, y0 - FINE / 2, px + pipWidth + FINE / 2, y0 + h + FINE / 2,
                    Palette.alpha(Palette.BACKGROUND, alpha), chamfer, true, true);
            chamferRect(canvas, px, y0, px + pipWidth, y0 + h, Palette.alpha(Palette.BORDER, alpha), chamfer, true, true);
            int ix0 = px + inset;
            int ix1 = px + pipWidth - inset;
            int iy0 = y0 + inset;
            int iy1 = y0 + h - inset;
            int innerW = Math.max(1, ix1 - ix0);
            chamferRect(canvas, ix0, iy0, ix1, iy1, Palette.alpha(Palette.EMPTY, alpha), chamfer, true, true);
            if (config.trail() && trailFrac > fillFrac) {
                int trailW = Math.round(innerW * trailFrac);
                chamferRect(canvas, ix0, iy0, ix0 + trailW, iy1, Palette.alpha(Palette.TRAIL, alpha), chamfer, true,
                        trailFrac >= 0.999F);
            }
            int filled = Math.round(innerW * fillFrac);
            if (filled > 0) {
                boolean full = fillFrac >= 0.999F;
                chamferRect(canvas, ix0, iy0, ix0 + filled, iy1, Palette.alpha(color, alpha), chamfer, true, full);
                int innerH = iy1 - iy0;
                if (height >= 3 && innerH >= 3) {
                    int band = Math.max(1, innerH * 3 / 10);
                    chamferRect(canvas, ix0, iy0, ix0 + filled, iy0 + band,
                            Palette.alpha(Palette.lerp(color, WHITE, 0.45F), alpha), chamfer, true, full);
                    chamferRect(canvas, ix0, iy1 - band, ix0 + filled, iy1,
                            Palette.alpha(Palette.darker(color, 0.35F), alpha), chamfer, true, full);
                }
                if (flash > 0.0F) {
                    chamferRect(canvas, ix0, iy0, ix0 + filled, iy1, Palette.alpha(WHITE, flash * 0.7F * alpha), chamfer,
                            true, full);
                }
            }
            if (heal > 0.0F) {
                canvas.fill(px, y0 - FINE, px + pipWidth, y0, Palette.alpha(Palette.HEAL, heal * alpha));
            }
        }
        canvas.pop();
    }

    private static void fillOutline(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                                int width, int height, float alpha, float partial) {
        float max = Math.max(tracked.max(), 0.001F);
        float shown = clamp(tracked.bar().shown(partial) / max, 0.0F, 1.0F);
        float trail = clamp(tracked.bar().trail(partial) / max, 0.0F, 1.0F);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int x0 = -w / 2;
        int y0 = -h;
        int chamfer = Math.max(1, Math.min(FINE, h / 4));
        chamferRect(canvas, x0 - FINE, y0 - FINE, x0 + w + FINE, y0 + h + FINE, Palette.alpha(0xE0000000, alpha), chamfer, true, true);
        chamferRect(canvas, x0, y0, x0 + w, y0 + h, Palette.alpha(0x50000000, alpha), chamfer, true, true);
        if (config.trail() && trail > shown) {
            int trailW = Math.round(w * trail);
            chamferRect(canvas, x0, y0, x0 + trailW, y0 + h, Palette.alpha(Palette.TRAIL, alpha), chamfer, true, trail >= 0.999F);
        }
        int color = config.colors() == ColorMode.HEALTH ? Palette.health(shown) : category.color();
        int filled = Math.round(w * shown);
        if (filled > 0) {
            boolean full = shown >= 0.999F;
            chamferRect(canvas, x0, y0, x0 + filled, y0 + h, Palette.alpha(color, alpha), chamfer, true, full);
            float flash = tracked.bar().flash(partial);
            if (flash > 0.0F) {
                chamferRect(canvas, x0, y0, x0 + filled, y0 + h, Palette.alpha(WHITE, flash * 0.7F * alpha), chamfer, true, full);
            }
        }
        float heal = tracked.bar().heal(partial);
        if (heal > 0.0F) {
            int glow = Palette.alpha(Palette.HEAL, heal * alpha);
            canvas.fill(x0 - FINE, y0 - FINE, x0 + w + FINE, y0, glow);
            canvas.fill(x0 - FINE, y0 + h, x0 + w + FINE, y0 + h + FINE, glow);
        }
        canvas.pop();
    }

    static int segments(float max) {
        if (max <= 40.0F) {
            return Math.max(1, (int) Math.ceil(max / 2.0F));
        }
        return 10;
    }

    private static void drawInfoRow(Canvas canvas, String health, int armor, int centerX, int y, float alpha, boolean centered) {
        String armorText = armor > 0 ? Integer.toString(armor) : "";
        int shieldWidth = armor > 0 ? 7 + canvas.textWidth(armorText) + (health.isEmpty() ? 0 : 6) : 0;
        int total = shieldWidth + canvas.textWidth(health);
        if (total == 0) {
            return;
        }
        int x = centered ? centerX - total / 2 : centerX;
        if (armor > 0) {
            shield(canvas, x, y, alpha);
            canvas.text(armorText, x + 7, y, Palette.alpha(0xFFC8D8F0, alpha));
            x += shieldWidth;
        }
        if (!health.isEmpty()) {
            canvas.text(health, x, y, Palette.alpha(WHITE, alpha));
        }
    }

    private static void shield(Canvas canvas, int x, int y, float alpha) {
        int edge = Palette.alpha(0xFF1A2A40, alpha);
        int face = Palette.alpha(0xFF9FB8D8, alpha);
        canvas.fill(x, y, x + 6, y + 5, edge);
        canvas.fill(x + 1, y + 5, x + 5, y + 7, edge);
        canvas.fill(x + 2, y + 7, x + 4, y + 8, edge);
        canvas.fill(x + 1, y + 1, x + 5, y + 5, face);
        canvas.fill(x + 2, y + 5, x + 4, y + 7, face);
    }

    private static void drawPopups(Canvas canvas, HeartlineConfig config, Projection projection, Player player, float partial) {
        int count = 0;
        for (Popup popup : Tracker.popups().all()) {
            Tracked tracked = Tracker.get(popup.entityId());
            if (tracked != null) {
                LivingEntity entity = tracked.entity();
                if (!tracked.visible() || config.isHidden(Entities.type(entity))
                        || !shows(config, Entities.category(entity, player))) {
                    continue;
                }
            }
            double dx = popup.x() - projection.cameraX();
            double dz = popup.z() - projection.cameraZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            double rx = flat < 0.001 ? 1.0 : -dz / flat;
            double rz = flat < 0.001 ? 0.0 : dx / flat;
            double x = popup.x() + rx * popup.drift();
            double y = popup.y() + 0.3 + popup.rise(partial);
            double z = popup.z() + rz * popup.drift();
            float scale = screenScale(projection, x, y, z, dx, dz, 0.22F / 9.0F);
            if (scale <= 0.0F) {
                continue;
            }
            float sx = OUT[0];
            float sy = OUT[1];
            scale = clamp(scale, 0.5F, 1.4F) * (float) config.numberScale() * popup.pop(partial);
            float alpha = popup.alpha(partial);
            String text = popupText(popup);
            int color = popup.kind() == Popup.Kind.HEAL ? HEAL : popup.kind() == Popup.Kind.CRIT ? CRIT : DAMAGE;
            canvas.push();
            canvas.translate(sx, sy);
            canvas.scale(scale);
            canvas.text(text, -canvas.textWidth(text) / 2, -canvas.lineHeight() / 2, Palette.alpha(color, alpha));
            canvas.pop();
            count++;
        }
        popupsDrawn = count;
    }

    static String popupText(Popup popup) {
        String amount = Numbers.amount(popup.amount());
        String text;
        switch (popup.kind()) {
            case HEAL:
                text = "+" + amount;
                break;
            case CRIT:
                text = amount + "!";
                break;
            default:
                text = amount;
                break;
        }
        if (popup.hits() > 1) {
            text += " ×" + popup.hits();
        }
        return text;
    }

    private static void drawPanel(Canvas canvas, HeartlineConfig config, Player player, float partial) {
        PanelPosition position = config.panel();
        Tracked tracked = Tracker.target();
        if (position == PanelPosition.OFF || tracked == null) {
            return;
        }
        LivingEntity entity = tracked.entity();
        Category category = Entities.category(entity, player);
        if (config.isHidden(Entities.type(entity))) {
            return;
        }
        panelDrawn = true;
        if (position == PanelPosition.CROSSHAIR) {
            canvas.push();
            canvas.translate(canvas.width() / 2.0F, canvas.height() / 2.0F + 14.0F);
            fillBar(canvas, config, tracked, category, 36, 3, 0.9F, partial);
            canvas.push();
            canvas.scale(TEXT);
            String health = Numbers.health(config.healthText(), tracked.health(), tracked.max());
            drawInfoRow(canvas, health, 0, 0, 3, 0.9F, true);
            canvas.pop();
            canvas.pop();
            return;
        }

        int width = 128;
        int height = 38;
        int x;
        if (position == PanelPosition.TOP_CENTER) {
            x = canvas.width() / 2 - width / 2;
        } else if (position == PanelPosition.TOP_RIGHT) {
            x = canvas.width() - width - 4;
        } else {
            x = 4;
        }
        int y = 4;
        canvas.fill(x, y, x + width, y + height, 0xA0000000);
        canvas.fill(x, y, x + 2, y + height, category.color());

        String name = Entities.name(entity);
        canvas.text(name, x + 6, y + 4, Palette.lerp(category.color(), WHITE, 0.55F));
        if (category == Category.PET) {
            String tag = Texts.translatable("heartline.panel.pet").getString();
            canvas.text(tag, x + width - 4 - canvas.textWidth(tag), y + 4, 0xFFB0B0B0);
        }

        canvas.push();
        canvas.translate(x + 6 + 58, y + 20);
        fillBar(canvas, config, tracked, category, 116, 5, 1.0F, partial);
        canvas.pop();

        HealthText style = config.healthText() == HealthText.OFF ? HealthText.VALUE : config.healthText();
        String health = Numbers.health(style, tracked.health(), tracked.max());
        int armor = config.armor() ? Entities.armor(entity) : 0;
        canvas.push();
        canvas.translate(x + 6, y + 24);
        drawInfoRow(canvas, health, armor, 0, 1, 1.0F, false);
        canvas.pop();

        if (config.hitsToKill() && category != Category.PET && category != Category.PLAYER) {
            float hit = Entities.attackDamage(player);
            int hits = Numbers.hitsToKill(tracked.health(), hit);
            if (hits > 0) {
                String text = Texts.translatable(hits == 1 ? "heartline.panel.hit" : "heartline.panel.hits", hits)
                        .getString();
                canvas.text(text, x + width - 4 - canvas.textWidth(text), y + 25, hits == 1 ? CRIT : 0xFFD0D0D0);
            }
        }
    }

    private static void drawNotices(Canvas canvas, float partial) {
        String notice = Tracker.notice();
        if (notice != null) {
            float alpha = Tracker.noticeAlpha(partial);
            int w = canvas.textWidth(notice);
            int x = canvas.width() / 2 - w / 2;
            int y = canvas.height() / 2 + 24;
            canvas.fill(x - 4, y - 3, x + w + 4, y + canvas.lineHeight() + 2, Palette.alpha(0x90000000, alpha));
            canvas.text(notice, x, y, Palette.alpha(WHITE, alpha));
        }
        String warning = Tracker.petWarning();
        if (warning != null) {
            float alpha = Tracker.petWarningAlpha(partial);
            float pulse = 0.5F + 0.5F * (float) Math.sin((Tracker.now() + partial) * 0.5F);
            int color = Palette.alpha(Palette.lerp(0xFFFF4040, 0xFFFFD0D0, pulse), alpha);
            int w = canvas.textWidth(warning);
            int x = canvas.width() / 2 - w / 2;
            int y = canvas.height() - 88;
            canvas.fill(x - 4, y - 3, x + w + 4, y + canvas.lineHeight() + 2, Palette.alpha(0xA0200000, alpha));
            canvas.text(warning, x, y, color);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int barsDrawn() {
        return barsDrawn;
    }

    public static int popupsDrawn() {
        return popupsDrawn;
    }

    public static boolean drew(int entityId) {
        return DRAWN.contains(entityId);
    }

    public static boolean panelDrawn() {
        return panelDrawn;
    }
}
