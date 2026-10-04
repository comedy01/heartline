package dev.heartline.mixin;

import dev.heartline.hud.Canvas;
import dev.heartline.hud.HeartlineHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class HudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void heartline$bars(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        HeartlineHud.render(mc, new Canvas(graphics, mc.font), partialTick);
    }
}
