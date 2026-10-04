package dev.heartline.client;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

final class AttackDamage {
    private AttackDamage() {
    }

    static double itemBonus(Player player) {
        double[] bonus = {0.0};
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                bonus[0] += modifier.amount();
            }
        });
        return bonus[0];
    }
}
