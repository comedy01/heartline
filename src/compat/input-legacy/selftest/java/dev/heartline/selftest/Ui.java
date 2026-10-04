package dev.heartline.selftest;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

final class Ui {
    private Ui() {
    }

    static void click(Screen screen, AbstractWidget widget) {
        double x = widget.getX() + widget.getWidth() / 2.0;
        double y = widget.getY() + widget.getHeight() / 2.0;
        screen.mouseClicked(x, y, 0);
        screen.mouseReleased(x, y, 0);
    }
}
