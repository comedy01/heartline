package dev.heartline.selftest.mixin;

import dev.heartline.selftest.HeartlineSelfTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftFrameMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void heartlineSelftest$frame(boolean advanceGameTime, CallbackInfo ci) {
        HeartlineSelfTest.frame();
    }
}
