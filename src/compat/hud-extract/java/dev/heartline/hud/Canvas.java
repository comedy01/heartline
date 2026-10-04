package dev.heartline.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Canvas {
    private final GuiGraphicsExtractor graphics;
    private final Font font;

    public Canvas(GuiGraphicsExtractor graphics, Font font) {
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
        graphics.pose().pushMatrix();
    }

    void pop() {
        graphics.pose().popMatrix();
    }

    void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    void scale(float scale) {
        graphics.pose().scale(scale, scale);
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
        graphics.text(font, text, x, y, color, true);
    }

    int textWidth(String text) {
        return font.width(text);
    }

    int lineHeight() {
        return font.lineHeight;
    }
}
