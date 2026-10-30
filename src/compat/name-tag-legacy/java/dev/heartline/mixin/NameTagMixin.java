package dev.heartline.mixin;

import dev.heartline.client.HeartlineClient;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class NameTagMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void heartline$hideName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && HeartlineClient.config().hidesVanillaNames()) {
            cir.setReturnValue(false);
        }
    }
}
