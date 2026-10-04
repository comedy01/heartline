package dev.heartline.core;

public final class BarAnimator {
    public static final int HOLD_TICKS = 12;
    public static final int FLASH_TICKS = 6;
    public static final int HEAL_TICKS = 10;

    private float target;
    private float shown;
    private float trail;
    private float previousShown;
    private float previousTrail;
    private int hold;
    private int flash;
    private int heal;

    public void reset(float health) {
        target = health;
        shown = health;
        trail = health;
        previousShown = health;
        previousTrail = health;
        hold = 0;
        flash = 0;
        heal = 0;
    }

    public void tick(float health, float max) {
        previousShown = shown;
        previousTrail = trail;
        if (flash > 0) {
            flash--;
        }
        if (heal > 0) {
            heal--;
        }
        if (health < target - 0.001F) {
            flash = FLASH_TICKS;
            hold = HOLD_TICKS;
            if (trail < shown) {
                trail = shown;
            }
        } else if (health > target + 0.001F) {
            heal = HEAL_TICKS;
        }
        target = health;
        shown += (health - shown) * 0.5F;
        if (Math.abs(health - shown) < 0.01F) {
            shown = health;
        }
        if (hold > 0) {
            hold--;
        } else if (trail > shown) {
            float step = Math.max(Math.max(max, 1.0F) * 0.015F, (trail - shown) * 0.12F);
            trail = Math.max(shown, trail - step);
        }
        if (trail < shown) {
            trail = shown;
        }
    }

    public float shown(float partial) {
        return previousShown + (shown - previousShown) * partial;
    }

    public float trail(float partial) {
        return previousTrail + (trail - previousTrail) * partial;
    }

    public float flash(float partial) {
        return Math.max(0.0F, flash - partial) / FLASH_TICKS;
    }

    public float heal(float partial) {
        return Math.max(0.0F, heal - partial) / HEAL_TICKS;
    }
}
