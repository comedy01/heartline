package dev.heartline.client;

import dev.heartline.core.BarAnimator;
import net.minecraft.world.entity.LivingEntity;

public final class Tracked {
    final LivingEntity entity;
    final BarAnimator bar = new BarAnimator();
    float health;
    float max;
    int lastHurt = Integer.MIN_VALUE / 2;
    boolean visible = true;
    boolean lowAlerted;

    Tracked(LivingEntity entity) {
        this.entity = entity;
        this.health = entity.getHealth();
        this.max = entity.getMaxHealth();
        this.bar.reset(health);
    }

    public LivingEntity entity() {
        return entity;
    }

    public BarAnimator bar() {
        return bar;
    }

    public float health() {
        return health;
    }

    public float max() {
        return max;
    }

    public int lastHurt() {
        return lastHurt;
    }

    public boolean visible() {
        return visible;
    }
}
