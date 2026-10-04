package dev.heartline.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public final class EntityTypes {
    private EntityTypes() {
    }

    public static Iterable<EntityType<?>> all() {
        return BuiltInRegistries.ENTITY_TYPE;
    }
}
