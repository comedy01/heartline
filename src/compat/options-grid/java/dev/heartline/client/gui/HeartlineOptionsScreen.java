package dev.heartline.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class HeartlineOptionsScreen extends OptionsSubScreen {
    private static final int ROW = 24;
    private static final int BOTTOM = 36;

    private final List<Row> placed = new ArrayList<>();
    private int rows;
    private int scroll;

    protected HeartlineOptionsScreen(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    protected abstract void addOptions();

    @Override
    protected void init() {
        placed.clear();
        rows = 0;
        addOptions();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        layout();
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 27, 200, 20)
                .build());
    }

    protected static Button button(Component label, int width, Button.OnPress onPress) {
        return Button.builder(label, onPress).width(width).build();
    }

    protected <T extends AbstractWidget> T tooltip(T widget, Component text) {
        widget.setTooltip(Tooltip.create(text));
        return widget;
    }

    protected void addHeader(Component text) {
        placed.add(new Row(addRenderableWidget(new Header(text)), rows, -155));
        rows++;
    }

    protected void addRow(AbstractWidget left, AbstractWidget right) {
        placed.add(new Row(addRenderableWidget(left), rows, -155));
        if (right != null) {
            placed.add(new Row(addRenderableWidget(right), rows, 5));
        }
        rows++;
    }

    private int top() {
        return height / 6 - 12;
    }

    private int maxScroll() {
        return Math.max(0, rows * ROW - (height - top() - BOTTOM));
    }

    private void layout() {
        for (Row row : placed) {
            int y = top() + row.row * ROW - scroll;
            row.widget.setPosition(width / 2 + row.offset, y);
            row.widget.visible = y >= top() - 2 && y + 20 <= height - BOTTOM + 2;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * ROW)));
        layout();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
    }

    private static final class Row {
        final AbstractWidget widget;
        final int row;
        final int offset;

        Row(AbstractWidget widget, int row, int offset) {
            this.widget = widget;
            this.row = row;
            this.offset = offset;
        }
    }

    private static final class Header extends AbstractWidget {
        Header(Component text) {
            super(0, 0, 310, 20, text);
            active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2,
                    getY() + (getHeight() - font.lineHeight) / 2 + 1, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }
}
