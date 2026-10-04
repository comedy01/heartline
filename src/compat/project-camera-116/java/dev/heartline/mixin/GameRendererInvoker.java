package dev.heartline.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("bobHurt")
    void heartline$bobHurt(PoseStack pose, float partialTick);

    @Invoker("bobView")
    void heartline$bobView(PoseStack pose, float partialTick);
}
