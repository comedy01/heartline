package dev.heartline.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;

public final class Canvas {
    private final PoseStack pose;
    private final Font font;

    public Canvas(PoseStack pose, Font font) {
        this.pose = pose;
        this.font = font;
    }

    int width() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    int height() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    void push() {
        pose.pushPose();
    }

    void pop() {
        pose.popPose();
    }

    void translate(float x, float y) {
        pose.translate(x, y, 0.0);
    }

    void scale(float scale) {
        pose.scale(scale, scale, 1.0F);
    }

    void fill(int x0, int y0, int x1, int y1, int color) {
        if ((color >>> 24) == 0) {
            return;
        }
        GuiComponent.fill(pose, x0, y0, x1, y1, color);
    }

    void text(String text, int x, int y, int color) {
        if ((color >>> 24) < 8) {
            return;
        }
        font.drawShadow(pose, text, x, y, color);
    }

    int textWidth(String text) {
        return font.width(text);
    }

    int lineHeight() {
        return font.lineHeight;
    }
}
