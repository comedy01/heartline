package dev.heartline.client;

import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityType;

public final class EntityTypes {
    private EntityTypes() {
    }

    public static Iterable<EntityType<?>> all() {
        return Registry.ENTITY_TYPE;
    }
}
