package dev.heartline.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

final class Owners {
    private Owners() {
    }

    static boolean ownedBy(Entity entity, Player player) {
        UUID owner = null;
        if (entity instanceof TamableAnimal) {
            owner = ((TamableAnimal) entity).getOwnerUUID();
        } else if (entity instanceof AbstractHorse) {
            owner = ((AbstractHorse) entity).getOwnerUUID();
        }
        return owner != null && owner.equals(player.getUUID());
    }
}
