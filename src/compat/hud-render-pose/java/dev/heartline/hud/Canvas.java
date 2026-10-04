package dev.heartline.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class Canvas {
    private final GuiGraphics graphics;
    private final Font font;

    public Canvas(GuiGraphics graphics, Font font) {
        this.graphics = graphics;
        this.font = font;
    }

    int width() {
        return graphics.guiWidth();
    }

    int height() {
        return graphics.guiHeight();
    }

    void push() {
        graphics.pose().pushPose();
    }

    void pop() {
        graphics.pose().popPose();
    }

    void translate(float x, float y) {
        graphics.pose().translate(x, y, 0.0F);
    }

    void scale(float scale) {
        graphics.pose().scale(scale, scale, 1.0F);
    }

    void fill(int x0, int y0, int x1, int y1, int color) {
        if ((color >>> 24) == 0) {
            return;
        }
        graphics.fill(x0, y0, x1, y1, color);
    }

    void text(String text, int x, int y, int color) {
        if ((color >>> 24) < 8) {
            return;
        }
        graphics.drawString(font, text, x, y, color, true);
    }

    int textWidth(String text) {
        return font.width(text);
    }

    int lineHeight() {
        return font.lineHeight;
    }
}
