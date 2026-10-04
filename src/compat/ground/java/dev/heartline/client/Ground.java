package dev.heartline.client;

import net.minecraft.world.entity.Entity;

public final class Ground {
    private Ground() {
    }

    public static boolean on(Entity entity) {
        return entity.onGround();
    }

    public static boolean glowing(Entity entity) {
        return entity.isCurrentlyGlowing();
    }
}
