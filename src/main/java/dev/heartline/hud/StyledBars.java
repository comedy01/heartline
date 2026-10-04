package dev.heartline.hud;

import dev.heartline.client.Tracked;
import dev.heartline.config.HeartlineConfig;
import dev.heartline.core.Category;
import dev.heartline.core.ColorMode;
import dev.heartline.core.Palette;

final class StyledBars {
    private static final int FINE = 4;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private StyledBars() {
    }

    private static final class State {
        final float shown;
        final float trail;
        final float flash;
        final float heal;
        final int color;
        final int segments;
        final boolean trailOn;

        State(HeartlineConfig config, Tracked tracked, Category category, float partial) {
            float max = Math.max(tracked.max(), 0.001F);
            shown = clamp(tracked.bar().shown(partial) / max, 0.0F, 1.0F);
            trail = clamp(tracked.bar().trail(partial) / max, 0.0F, 1.0F);
            flash = tracked.bar().flash(partial);
            heal = tracked.bar().heal(partial);
            color = config.colors() == ColorMode.HEALTH ? Palette.health(shown) : category.color();
            segments = HeartlineHud.segments(max);
            trailOn = config.trail() && trail > shown;
        }
    }

    static void glass(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                      int width, int height, float alpha, float partial) {
        State s = new State(config, tracked, category, partial);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int x0 = -w / 2;
        int x1 = x0 + w;
        int y0 = -h;
        int y1 = 0;
        int r = Math.min(h / 2, FINE + FINE / 2);
        rounded(canvas, x0 - FINE, y0 - FINE / 2, x1 + FINE, y1 + FINE + FINE / 2, r + FINE, x1 + FINE, a(0x48000000, alpha));
        rounded(canvas, x0 - FINE / 2, y0 - FINE / 2, x1 + FINE / 2, y1 + FINE / 2, r + FINE / 2, x1 + FINE / 2, a(0xD00A0D12, alpha));
        rounded(canvas, x0, y0, x1, y1, r, x1, a(0xB0232A35, alpha));
        int fx1 = x0 + Math.round(w * s.shown);
        if (s.trailOn) {
            rounded(canvas, x0, y0, x1, y1, r, x0 + Math.round(w * s.trail), a(Palette.TRAIL, 0.75F * alpha));
        }
        if (fx1 > x0) {
            rounded(canvas, x0, y0, x1, y1, r, fx1, a(s.color, alpha));
            int rows = Math.max(1, h);
            for (int i = 0; i < rows; i++) {
                float t = (float) i / rows;
                int c = t < 0.5F
                        ? a(WHITE, 0.32F * (1.0F - t * 2.0F) * alpha)
                        : a(BLACK, 0.3F * (t - 0.5F) * 2.0F * alpha);
                roundedRow(canvas, x0, y0, x1, y1, r, y0 + i, x0, fx1, c);
            }
            if (s.flash > 0.0F) {
                rounded(canvas, x0, y0, x1, y1, r, fx1, a(WHITE, s.flash * 0.7F * alpha));
            }
        }
        int gloss = Math.max(1, h * 2 / 5);
        for (int i = 0; i < gloss; i++) {
            float k = 1.0F - (float) i / gloss;
            roundedRow(canvas, x0, y0, x1, y1, r, y0 + i, x0, x1, a(WHITE, 0.22F * k * k * alpha));
        }
        canvas.fill(x0 + r, y0 - FINE / 2, x1 - r, y0 - FINE / 2 + 1, a(0x40FFFFFF, alpha));
        if (s.heal > 0.0F) {
            rounded(canvas, x0 - FINE, y0 - FINE, x1 + FINE, y0, r, x1 + FINE, a(Palette.HEAL, 0.5F * s.heal * alpha));
            rounded(canvas, x0 - FINE, y1, x1 + FINE, y1 + FINE, r, x1 + FINE, a(Palette.HEAL, 0.5F * s.heal * alpha));
        }
        canvas.pop();
    }

    static void neon(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                     int width, int height, float alpha, float partial) {
        State s = new State(config, tracked, category, partial);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int x0 = -w / 2;
        int x1 = x0 + w;
        int mid = -h / 2;
        int core = Math.max(2, h / 4);
        int cy0 = mid - core / 2;
        int cy1 = cy0 + core;
        int glow = Math.max(2, (h - core) / 2);
        int tube = s.heal > 0.0F ? Palette.lerp(s.color, Palette.HEAL, s.heal) : s.color;
        canvas.fill(x0 - 1, cy0 - 1, x1 + 1, cy1 + 1, a(0x90000000, alpha));
        canvas.fill(x0, cy0, x1, cy1, a(Palette.lerp(0xFF23272E, tube, 0.06F), alpha));
        int fx1 = x0 + Math.round(w * s.shown);
        if (s.trailOn) {
            canvas.fill(fx1, cy0, x0 + Math.round(w * s.trail), cy1, a(Palette.TRAIL, 0.6F * alpha));
        }
        if (fx1 > x0) {
            for (int i = glow; i >= 1; i--) {
                float k = 1.0F - (float) (i - 1) / glow;
                int c = a(tube, (0.05F + 0.17F * k * k) * alpha);
                int pad = i * 2 / 3;
                canvas.fill(x0 - pad, cy0 - i, fx1 + pad, cy0, c);
                canvas.fill(x0 - pad, cy1, fx1 + pad, cy1 + i, c);
                canvas.fill(x0 - i, cy0, x0, cy1, c);
                canvas.fill(fx1, cy0, fx1 + i, cy1, c);
            }
            canvas.fill(x0, cy0, fx1, cy1, a(Palette.lerp(tube, WHITE, 0.2F), alpha));
            int hot = Math.max(1, core / 3);
            int hy = cy0 + (core - hot) / 2;
            canvas.fill(x0 + 1, hy, fx1 - 1, hy + hot, a(Palette.lerp(tube, WHITE, 0.75F), alpha));
            if (s.flash > 0.0F) {
                canvas.fill(x0, cy0 - glow, fx1, cy1 + glow, a(WHITE, s.flash * 0.35F * alpha));
                canvas.fill(x0, cy0, fx1, cy1, a(WHITE, s.flash * 0.8F * alpha));
            }
            if (s.shown < 0.999F) {
                canvas.fill(fx1 - 1, cy0 - 2, fx1 + 1, cy1 + 2, a(WHITE, 0.9F * alpha));
            }
        }
        canvas.pop();
    }

    static void capsule(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                        int width, int height, float alpha, float partial) {
        State s = new State(config, tracked, category, partial);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int x0 = -w / 2;
        int x1 = x0 + w;
        int y0 = -h;
        int y1 = 0;
        int pad = Math.max(2, h / 7);
        rounded(canvas, x0 - FINE, y0 - FINE, x1 + FINE, y1 + FINE, (h + 2 * FINE) / 2, x1 + FINE, a(0xE0000000, alpha));
        rounded(canvas, x0, y0, x1, y1, h / 2, x1, a(0xE01C2026, alpha));
        int ix0 = x0 + pad;
        int ix1 = x1 - pad;
        int iy0 = y0 + pad;
        int iy1 = y1 - pad;
        int ih = iy1 - iy0;
        int iw = ix1 - ix0;
        int ir = ih / 2;
        if (s.trailOn) {
            pillTo(canvas, ix0, iy0, iy1, ir, ix0 + Math.round(iw * s.trail), a(Palette.lerp(Palette.TRAIL, BLACK, 0.15F), alpha));
        }
        int fx1 = ix0 + Math.round(iw * s.shown);
        if (fx1 > ix0) {
            pillTo(canvas, ix0, iy0, iy1, ir, fx1, a(s.color, alpha));
            int band = Math.max(1, ih / 4);
            int hx0 = ix0 + ir / 2;
            int hx1 = fx1 - ir / 2;
            if (hx1 > hx0) {
                rounded(canvas, hx0, iy0 + band / 2, hx1, iy0 + band / 2 + band, band / 2, hx1,
                        a(Palette.lerp(s.color, WHITE, 0.55F), 0.8F * alpha));
            }
            int shade = Math.max(1, ih / 5);
            for (int i = 0; i < shade; i++) {
                int y = iy1 - shade + i;
                roundedRow(canvas, ix0, iy0, Math.max(fx1, ix0 + ih), iy1, ir, y, ix0, fx1,
                        a(BLACK, 0.18F * (i + 1) / shade * alpha));
            }
            if (s.flash > 0.0F) {
                pillTo(canvas, ix0, iy0, iy1, ir, fx1, a(WHITE, s.flash * 0.7F * alpha));
            }
        }
        if (s.heal > 0.0F) {
            int glow = a(Palette.HEAL, s.heal * alpha);
            rounded(canvas, x0 - FINE, y0 - FINE, x1 + FINE, y1 + FINE, (h + 2 * FINE) / 2, x1 + FINE, a(Palette.HEAL, 0.25F * s.heal * alpha));
            canvas.fill(x0 + h / 2, y0 - FINE, x1 - h / 2, y0 - FINE + 2, glow);
        }
        canvas.pop();
    }

    static void gauge(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                      int width, int height, float alpha, float partial) {
        State s = new State(config, tracked, category, partial);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int x0 = -w / 2;
        int x1 = x0 + w;
        int y0 = -h;
        int y1 = 0;
        int barH = Math.max(FINE, h * 11 / 20);
        int by1 = y0 + barH;
        int tickTop = by1 + Math.max(1, FINE / 2);
        rounded(canvas, x0 - FINE, y0 - FINE, x1 + FINE, y1 + FINE / 2, FINE, x1 + FINE, a(0xB80C0E12, alpha));
        canvas.fill(x0, y0, x1, by1, a(0xFF2A2F37, alpha));
        int fx1 = x0 + Math.round(w * s.shown);
        if (s.trailOn) {
            canvas.fill(fx1, y0, x0 + Math.round(w * s.trail), by1, a(Palette.TRAIL, 0.8F * alpha));
        }
        if (fx1 > x0) {
            canvas.fill(x0, y0, fx1, by1, a(s.color, alpha));
            int band = Math.max(1, barH / 4);
            canvas.fill(x0, y0, fx1, y0 + band, a(Palette.lerp(s.color, WHITE, 0.4F), alpha));
            if (s.flash > 0.0F) {
                canvas.fill(x0, y0, fx1, by1, a(WHITE, s.flash * 0.7F * alpha));
            }
        }
        int lit = a(Palette.lerp(s.color, WHITE, 0.25F), alpha);
        int dim = a(0x50FFFFFF, alpha);
        int full = y1 - tickTop;
        for (int i = 0; i <= s.segments; i++) {
            float f = (float) i / s.segments;
            int x = x0 + Math.round(w * f);
            boolean quarter = isQuarter(i, s.segments);
            int len = quarter ? full : Math.max(1, full / 2);
            int tx = Math.min(x1 - 2, Math.max(x0, x - 1));
            canvas.fill(tx, tickTop, tx + 2, tickTop + len, f <= s.shown + 0.0001F ? lit : dim);
        }
        if (s.heal > 0.0F) {
            canvas.fill(x0 - FINE, y0 - FINE, x1 + FINE, y0 - FINE + 2, a(Palette.HEAL, s.heal * alpha));
        }
        canvas.pop();
    }

    static void bracket(Canvas canvas, HeartlineConfig config, Tracked tracked, Category category,
                        int width, int height, float alpha, float partial) {
        State s = new State(config, tracked, category, partial);
        canvas.push();
        canvas.scale(1.0F / FINE);
        int w = width * FINE;
        int h = height * FINE;
        int x0 = -w / 2;
        int x1 = x0 + w;
        int y0 = -h;
        int y1 = 0;
        int gap = Math.max(2, FINE * 3 / 4);
        int line = Math.max(2, h - 2 * gap - 2);
        int ly0 = y0 + (h - line) / 2;
        int ly1 = ly0 + line;
        int lx0 = x0 + gap + 1;
        int lx1 = x1 - gap - 1;
        int lw = lx1 - lx0;
        int frame = a(s.heal > 0.0F ? Palette.lerp(0xE6FFFFFF, Palette.HEAL, s.heal) : 0xE6FFFFFF, alpha);
        int arm = Math.max(FINE, h / 2);
        int t = Math.max(1, FINE / 3);
        bracketPair(canvas, x0, y0, x1, y1, arm, t + 1, a(0x80000000, alpha), 1);
        bracketPair(canvas, x0, y0, x1, y1, arm, t, frame, 0);
        canvas.fill(lx0 - 1, ly0 - 1, lx1 + 1, ly1 + 1, a(0x90000000, alpha));
        canvas.fill(lx0, ly0, lx1, ly1, a(0x38FFFFFF, alpha));
        int fx1 = lx0 + Math.round(lw * s.shown);
        if (s.trailOn) {
            canvas.fill(fx1, ly0, lx0 + Math.round(lw * s.trail), ly1, a(Palette.TRAIL, 0.7F * alpha));
        }
        if (fx1 > lx0) {
            canvas.fill(lx0, ly0, fx1, ly1, a(s.color, alpha));
            canvas.fill(lx0, ly0, fx1, ly0 + Math.max(1, line / 3), a(Palette.lerp(s.color, WHITE, 0.35F), alpha));
            if (s.flash > 0.0F) {
                canvas.fill(lx0, ly0, fx1, ly1, a(WHITE, s.flash * 0.7F * alpha));
            }
        }
        if (s.shown > 0.0F && s.shown < 0.999F) {
            int mx = Math.max(lx0, fx1 - 1);
            canvas.fill(mx - 1, ly0 - gap, mx + 2, ly1 + gap, a(0x90000000, alpha));
            canvas.fill(mx, ly0 - gap + 1, mx + 1, ly1 + gap - 1, a(WHITE, alpha));
        }
        canvas.pop();
    }

    private static void bracketPair(Canvas canvas, int x0, int y0, int x1, int y1, int arm, int t, int color, int grow) {
        int ax0 = x0 - grow;
        int ax1 = x1 + grow;
        int ay0 = y0 - grow;
        int ay1 = y1 + grow;
        canvas.fill(ax0, ay0, ax0 + t, ay1, color);
        canvas.fill(ax0 + t, ay0, ax0 + arm, ay0 + t, color);
        canvas.fill(ax0 + t, ay1 - t, ax0 + arm, ay1, color);
        canvas.fill(ax1 - t, ay0, ax1, ay1, color);
        canvas.fill(ax1 - arm, ay0, ax1 - t, ay0 + t, color);
        canvas.fill(ax1 - arm, ay1 - t, ax1 - t, ay1, color);
    }

    private static boolean isQuarter(int i, int segments) {
        return i * 4 % segments == 0 || i == 0 || i == segments;
    }

    private static void pillTo(Canvas canvas, int x0, int y0, int y1, int r, int x1, int color) {
        int h = y1 - y0;
        rounded(canvas, x0, y0, Math.max(x1, x0 + h), y1, r, x1, color);
    }

    private static void rounded(Canvas canvas, int x0, int y0, int x1, int y1, int r, int clip, int color) {
        int h = y1 - y0;
        if (h <= 0 || x1 <= x0 || clip <= x0) {
            return;
        }
        r = Math.max(0, Math.min(r, Math.min(h / 2, (x1 - x0) / 2)));
        for (int y = y0; y < y0 + r; y++) {
            roundedRow(canvas, x0, y0, x1, y1, r, y, x0, clip, color);
        }
        canvas.fill(x0, y0 + r, Math.min(x1, clip), y1 - r, color);
        for (int y = y1 - r; y < y1; y++) {
            roundedRow(canvas, x0, y0, x1, y1, r, y, x0, clip, color);
        }
    }

    private static void roundedRow(Canvas canvas, int x0, int y0, int x1, int y1, int r, int y, int from, int to, int color) {
        int inset = 0;
        if (r > 0) {
            float dy;
            if (y < y0 + r) {
                dy = (y0 + r) - (y + 0.5F);
            } else if (y >= y1 - r) {
                dy = (y + 0.5F) - (y1 - r);
            } else {
                dy = 0.0F;
            }
            if (dy > 0.0F) {
                inset = Math.round(r - (float) Math.sqrt(Math.max(0.0F, r * r - dy * dy)));
            }
        }
        int a = Math.max(x0 + inset, from);
        int b = Math.min(x1 - inset, to);
        if (b > a) {
            canvas.fill(a, y, b, y + 1, color);
        }
    }

    private static int a(int color, float alpha) {
        return Palette.alpha(color, alpha);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
