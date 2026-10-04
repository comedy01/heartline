package dev.heartline.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

final class Owners {
    private Owners() {
    }

    static boolean ownedBy(Entity entity, Player player) {
        if (entity instanceof OwnableEntity ownable) {
            java.util.UUID owner = ownable.getOwnerUUID();
            return owner != null && owner.equals(player.getUUID());
        }
        return false;
    }
}
