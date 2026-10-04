package dev.heartline.showcase.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void hlShowcase$setX(double x);

    @Accessor("ypos")
    void hlShowcase$setY(double y);
}
