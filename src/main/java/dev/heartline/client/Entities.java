package dev.heartline.client;

import dev.heartline.core.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Entities {
    public static final float BOSS_HEALTH = 150.0F;

    private Entities() {
    }

    public static boolean skip(LivingEntity entity) {
        if (entity instanceof ArmorStand) {
            return true;
        }
        if (entity instanceof Player player && player.isSpectator()) {
            return true;
        }
        return entity.isInvisible() && !Ground.glowing(entity);
    }

    public static Category category(LivingEntity entity, Player self) {
        if (self != null && Owners.ownedBy(entity, self)) {
            return Category.PET;
        }
        if (entity instanceof Player) {
            return Category.PLAYER;
        }
        if (entity instanceof WitherBoss || entity instanceof EnderDragon || entity.getMaxHealth() >= BOSS_HEALTH) {
            return Category.BOSS;
        }
        if (entity instanceof NeutralMob) {
            return Category.NEUTRAL;
        }
        if (entity instanceof Enemy) {
            return Category.HOSTILE;
        }
        return Category.PASSIVE;
    }

    public static String type(Entity entity) {
        return String.valueOf(EntityType.getKey(entity.getType()));
    }

    public static String name(Entity entity) {
        return entity.getDisplayName().getString();
    }

    public static String typeName(Entity entity) {
        return entity.getType().getDescription().getString();
    }

    public static int armor(LivingEntity entity) {
        return entity.getArmorValue();
    }

    public static boolean hasAttackDamage(LivingEntity entity) {
        return entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    public static float attackDamage(Player player) {
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double itemBonus = AttackDamage.itemBonus(player);
        return (float) Math.max(0.0, base + itemBonus);
    }

    public static boolean nameTagShown(LivingEntity entity) {
        return entity instanceof Player || entity.shouldShowName()
                || (entity.hasCustomName() && Minecraft.getInstance().crosshairPickEntity == entity);
    }

    public static boolean canSee(Level level, Vec3 eye, LivingEntity entity, Entity viewer) {
        Vec3 center = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
        Vec3 top = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ());
        return clear(level, eye, top, viewer) || clear(level, eye, center, viewer);
    }

    private static boolean clear(Level level, Vec3 from, Vec3 to, Entity viewer) {
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static boolean canCrit(Player player, Entity target) {
        return player.getAttackStrengthScale(0.5F) > 0.9F
                && player.fallDistance > 0.0F
                && !Ground.on(player)
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isSprinting()
                && target instanceof LivingEntity;
    }
}
