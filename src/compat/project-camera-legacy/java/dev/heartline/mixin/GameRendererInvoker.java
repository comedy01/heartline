package dev.heartline.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("bobHurt")
    void heartline$bobHurt(CameraRenderState camera, PoseStack pose);

    @Invoker("bobView")
    void heartline$bobView(CameraRenderState camera, PoseStack pose);
}
