package dev.heartline.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

final class Owners {
    private Owners() {
    }

    static boolean ownedBy(Entity entity, Player player) {
        if (entity instanceof OwnableEntity ownable) {
            EntityReference<LivingEntity> owner = ownable.getOwnerReference();
            return owner != null && owner.getUUID().equals(player.getUUID());
        }
        return false;
    }
}
