package dev.heartline.client.gui;

import net.minecraft.client.gui.components.EditBox;

final class Focus {
    private Focus() {
    }

    static void on(EditBox box) {
        box.setFocus(true);
    }
}
