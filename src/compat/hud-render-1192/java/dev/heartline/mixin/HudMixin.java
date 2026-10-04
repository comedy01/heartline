package dev.heartline.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.heartline.hud.Canvas;
import dev.heartline.hud.HeartlineHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class HudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void heartline$bars(PoseStack pose, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        HeartlineHud.render(mc, new Canvas(pose, mc.font), partialTick);
    }
}
