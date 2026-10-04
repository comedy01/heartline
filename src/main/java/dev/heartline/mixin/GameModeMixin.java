package dev.heartline.mixin;

import dev.heartline.client.Tracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
abstract class GameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void heartline$attack(Player player, Entity target, CallbackInfo ci) {
        Tracker.onAttack(player, target);
    }
}
