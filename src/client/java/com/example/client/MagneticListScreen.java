package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class MagneticListScreen extends Screen {

    protected static final int PANEL_W = 420;
    protected static final int PANEL_H = 330;
    protected static final int ROWS = 6;
    protected static final int ROW_H = 26;
    protected static final int ROW_GAP = 4;

    protected static final int LIST_Y = 52;
    protected static final int NAV_Y = 240;
    protected static final int FOOT_Y = 274;
    protected static final int FOOT_BTN_W = 124;
    protected static final int FOOT_GAP = 8;

    protected final Screen parent;
    protected int selected = -1;
    private int page = 0;
    private long openTime;

    protected MagneticListScreen(Screen parent, String title) {
        super(Component.literal(title));
        this.parent = parent;
    }

    // Do nadpisania w podklasach
    protected abstract String headerTitle();
    protected abstract String headerSub();
    protected abstract void onInit();
    protected abstract int entryCount();
    protected abstract String entryName(int i);
    protected abstract String entryHint(int i);
    protected abstract String emptyLine1();
    protected abstract String emptyLine2();
    protected abstract String playLabel();
    protected abstract String extraLabel();
    protected abstract void onPlay(int index);
    protected abstract void onExtra();

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
        this.page = 0;
        this.selected = -1;
        onInit();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        MagneticUi.navigate(() -> Minecraft.getInstance().setScreen(parent));
    }

    // ---------------------------------------------------------------------
    // Geometria
    // ---------------------------------------------------------------------

    protected int panelX() { return (this.width - PANEL_W) / 2; }
    protected int panelY() { return (this.height - PANEL_H) / 2; }

    private int pageCount() {
        return Math.max(1, (entryCount() + ROWS - 1) / ROWS);
    }

    private int rowRelY(int visibleIndex) {
        return LIST_Y + visibleIndex * (ROW_H + ROW_GAP);
    }

    private boolean hit(double mx, double my, int relX, int relY, int w, int h) {
        return MagneticUi.isInside(mx, my, panelX() + relX, panelY() + relY, w, h);
    }

    private void press(int relX, int relY, int w, int h) {
        MagneticUi.press(panelX() + relX, panelY() + relY, w, h);
    }

    // ---------------------------------------------------------------------
    // Rysowanie
    // ---------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float fade = MagneticUi.entrance(openTime, 450) * MagneticUi.exitAlpha();
        int px = panelX();
        int py = panelY();
        int count = entryCount();
        int secondary = ElasticAnimation.getSecondaryColor();
        int grey = ElasticAnimation.withAlpha(0xFF94A3B8, (int) (255 * fade));

        MagneticUi.drawBackground(g, this.width, this.height, mouseX, mouseY, fade);
        MagneticUi.drawPopPanel(g, px, py, PANEL_W, PANEL_H, fade, openTime);
        MagneticUi.drawHeader(g, this.font, headerTitle(), headerSub(), this.width / 2, py + 14, fade);

        if (count == 0) {
            String l1 = emptyLine1();
            String l2 = emptyLine2();
            g.drawString(this.font, l1, this.width / 2 - this.font.width(l1) / 2, py + 110, grey, true);
            g.drawString(this.font, l2, this.width / 2 - this.font.width(l2) / 2, py + 126, grey, true);
        }

        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx >= count) break;
            int ry = rowRelY(i);
            boolean hovered = hit(mouseX, mouseY, 16, ry, PANEL_W - 32, ROW_H);
            MagneticUi.drawButton(g, this.font, px + 16, py + ry, PANEL_W - 32, ROW_H,
                    entryName(idx), hovered, idx == selected, fade);
        }

        if (selected >= 0 && selected < count) {
            String hint = entryHint(selected);
            if (hint != null && !hint.isEmpty()) {
                g.drawString(this.font, hint, px + PANEL_W / 2 - this.font.width(hint) / 2, py + NAV_Y - 14,
                        ElasticAnimation.withAlpha(secondary, (int) (255 * fade)), true);
            }
        }

        boolean canPrev = page > 0;
        boolean canNext = (page + 1) * ROWS < count;
        MagneticUi.drawButton(g, this.font, px + 16, py + NAV_Y, 90, 24, "< PREV",
                canPrev && hit(mouseX, mouseY, 16, NAV_Y, 90, 24), false, fade);
        MagneticUi.drawButton(g, this.font, px + PANEL_W - 106, py + NAV_Y, 90, 24, "NEXT >",
                canNext && hit(mouseX, mouseY, PANEL_W - 106, NAV_Y, 90, 24), false, fade);

        String pageText = "PAGE " + (page + 1) + " / " + pageCount();
        g.drawString(this.font, pageText, px + PANEL_W / 2 - this.font.width(pageText) / 2, py + NAV_Y + 8,
                ElasticAnimation.withAlpha(secondary, (int) (255 * fade)), true);

        int x0 = 16;
        int x1 = x0 + FOOT_BTN_W + FOOT_GAP;
        int x2 = x1 + FOOT_BTN_W + FOOT_GAP;
        boolean canPlay = selected >= 0 && selected < count;

        MagneticUi.drawButton(g, this.font, px + x0, py + FOOT_Y, FOOT_BTN_W, 26, playLabel(),
                canPlay && hit(mouseX, mouseY, x0, FOOT_Y, FOOT_BTN_W, 26), canPlay, fade);
        MagneticUi.drawButton(g, this.font, px + x1, py + FOOT_Y, FOOT_BTN_W, 26, extraLabel(),
                hit(mouseX, mouseY, x1, FOOT_Y, FOOT_BTN_W, 26), false, fade);
        MagneticUi.drawButton(g, this.font, px + x2, py + FOOT_Y, FOOT_BTN_W, 26, "BACK",
                hit(mouseX, mouseY, x2, FOOT_Y, FOOT_BTN_W, 26), false, fade);
    }

    // ---------------------------------------------------------------------
    // Kliknięcia
    // ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || MagneticUi.isExiting()) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        int count = entryCount();
        int start = page * ROWS;

        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx >= count) break;
            int ry = rowRelY(i);
            if (hit(mx, my, 16, ry, PANEL_W - 32, ROW_H)) {
                press(16, ry, PANEL_W - 32, ROW_H);
                selected = idx;
                return true;
            }
        }

        if (page > 0 && hit(mx, my, 16, NAV_Y, 90, 24)) {
            press(16, NAV_Y, 90, 24);
            page--;
            return true;
        }
        if ((page + 1) * ROWS < count && hit(mx, my, PANEL_W - 106, NAV_Y, 90, 24)) {
            press(PANEL_W - 106, NAV_Y, 90, 24);
            page++;
            return true;
        }

        int x0 = 16;
        int x1 = x0 + FOOT_BTN_W + FOOT_GAP;
        int x2 = x1 + FOOT_BTN_W + FOOT_GAP;
        boolean canPlay = selected >= 0 && selected < count;

        if (canPlay && hit(mx, my, x0, FOOT_Y, FOOT_BTN_W, 26)) {
            press(x0, FOOT_Y, FOOT_BTN_W, 26);
            onPlay(selected);
            return true;
        }
        if (hit(mx, my, x1, FOOT_Y, FOOT_BTN_W, 26)) {
            press(x1, FOOT_Y, FOOT_BTN_W, 26);
            onExtra();
            return true;
        }
        if (hit(mx, my, x2, FOOT_Y, FOOT_BTN_W, 26)) {
            press(x2, FOOT_Y, FOOT_BTN_W, 26);
            onClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }
}