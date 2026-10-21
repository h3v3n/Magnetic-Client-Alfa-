package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MagneticThemeCreatorScreen extends Screen {

    private static final int W = 360;
    private static final int[] PRESETS = {
            0xFF9D4EDD, 0xFF00E5FF, 0xFFFF2A85, 0xFFFFD700, 0xFFFF1744, 0xFF00E676,
            0xFF2979FF, 0xFF10B981, 0xFFFFFFFF, 0xFF000000, 0xFFFF85A1, 0xFF4CC9F0
    };
    private static final String[] CHANNELS = {"RED", "GREEN", "BLUE"};

    private final Screen parent;
    private int colA = ModConfig.customThemeA;
    private int colB = ModConfig.customThemeB;
    private int slot = 0; // 0 = A, 1 = B
    private int dragChan = -1;
    private long openTime;

    public MagneticThemeCreatorScreen(Screen parent) {
        super(Component.literal("Custom Theme Creator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        back();
    }

    // ---------------------------------------------------------------------
    // Geometria
    // ---------------------------------------------------------------------

    private int gx() { return this.width / 2 - W / 2; }
    private int gy() { return this.height / 2 - 120; }

    private int cur() { return slot == 0 ? colA : colB; }

    private void setCur(int c) {
        if (slot == 0) colA = c; else colB = c;
    }

    private int channel(int c, int i) {
        return switch (i) {
            case 0 -> (c >> 16) & 0xFF;
            case 1 -> (c >> 8) & 0xFF;
            default -> c & 0xFF;
        };
    }

    private void setChannel(int i, int v) {
        int c = cur();
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        switch (i) {
            case 0 -> r = v;
            case 1 -> g = v;
            default -> b = v;
        }
        setCur(0xFF000000 | (r << 16) | (g << 8) | b);
    }

    // ---------------------------------------------------------------------
    // Rysowanie
    // ---------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        float fade = MagneticUi.entrance(openTime, 450) * MagneticUi.exitAlpha();
        int primary = ElasticAnimation.getPrimaryColor(0);
        int gx = gx();
        int gy = gy();

        MagneticUi.drawBackground(g, this.width, this.height, mx, my, fade);
        MagneticUi.drawHeader(g, this.font, "CUSTOM THEME",
                "PICK TWO COLORS • SEE THE BLEND", this.width / 2, gy - 40, fade);

        // Karty A i B
        drawCard(g, gx, gy, 170, 56, 0, "COLOR A", colA, mx, my, primary, fade);
        drawCard(g, gx + 190, gy, 170, 56, 1, "COLOR B", colB, mx, my, primary, fade);

        // Suwaki RGB dla wybranego koloru
        for (int i = 0; i < CHANNELS.length; i++) {
            int sy = gy + 70 + i * 24;
            boolean hov = MagneticUi.isInside(mx, my, gx, sy, W, 20);
            MagneticEditorUi.slider(g, this.font, gx, sy, W, 20,
                    CHANNELS[i], channel(cur(), i), 255, primary, hov);
        }

        // Presety
        int py = gy + 146;
        for (int i = 0; i < PRESETS.length; i++) {
            int x = gx + 6 + i * 29;
            boolean hov = MagneticUi.isInside(mx, my, x, py, 22, 22);
            MagneticEditorUi.swatch(g, x, py, 22, PRESETS[i], PRESETS[i] == cur() || hov, primary);
        }

        // Gradient - podgląd mieszania obu kolorów
        int by = gy + 180;
        int segs = 60;
        int segW = W / segs;
        for (int s = 0; s < segs; s++) {
            float t = s / (float) (segs - 1);
            int col = ElasticAnimation.blendColors(colA, colB, t);
            g.fill(gx + s * segW, by, gx + s * segW + segW, by + 20, ElasticAnimation.withAlpha(col, (int) (255 * fade)));
        }
        g.fill(gx - 1, by - 1, gx + W + 1, by, 0xFF2A3446);
        g.fill(gx - 1, by + 20, gx + W + 1, by + 21, 0xFF2A3446);
        String preview = "PREVIEW";
        g.drawString(this.font, preview, gx + W / 2 - this.font.width(preview) / 2, by + 6, 0xFFFFFFFF, true);

        // Przyciski
        int btnY = gy + 214;
        MagneticUi.drawButton(g, this.font, gx, btnY, 170, 24, "APPLY THEME",
                MagneticUi.isInside(mx, my, gx, btnY, 170, 24), true, fade);
        MagneticUi.drawButton(g, this.font, gx + 190, btnY, 170, 24, "BACK",
                MagneticUi.isInside(mx, my, gx + 190, btnY, 170, 24), false, fade);
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h, int idx, String label,
                          int color, int mx, int my, int primary, float fade) {
        boolean sel = slot == idx;
        boolean hov = MagneticUi.isInside(mx, my, x, y, w, h);
        int border = sel ? ElasticAnimation.withAlpha(primary, (int) (220 * fade)) : 0x552A354B;
        SmoothUiRenderer.drawSmoothCard(g, x, y, w, h, 9,
                ElasticAnimation.withAlpha(hov ? 0xFF171F30 : 0xFF0E131F, (int) (230 * fade)),
                border, 0);

        g.fill(x + 10, y + 10, x + 46, y + 46, ElasticAnimation.withAlpha(color, (int) (255 * fade)));
        g.drawString(this.font, label, x + 56, y + 14,
                ElasticAnimation.withAlpha(0xFFE2E8F0, (int) (255 * fade)), true);
        String hex = String.format("#%06X", color & 0xFFFFFF);
        g.drawString(this.font, hex, x + 56, y + 28,
                ElasticAnimation.withAlpha(0xFF94A3B8, (int) (255 * fade)), true);
    }

    // ---------------------------------------------------------------------
    // Myszka
    // ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (MagneticUi.isExiting()) return true;
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        double mx = event.x();
        double my = event.y();
        int gx = gx();
        int gy = gy();

        if (MagneticUi.isInside(mx, my, gx, gy, 170, 56)) {
            slot = 0;
            return true;
        }
        if (MagneticUi.isInside(mx, my, gx + 190, gy, 170, 56)) {
            slot = 1;
            return true;
        }

        for (int i = 0; i < CHANNELS.length; i++) {
            int sy = gy + 70 + i * 24;
            if (MagneticUi.isInside(mx, my, gx, sy, W, 20)) {
                dragChan = i;
                setChannel(i, MagneticEditorUi.valueFromMouse(mx, gx, W, 255));
                return true;
            }
        }

        int py = gy + 146;
        for (int i = 0; i < PRESETS.length; i++) {
            int x = gx + 6 + i * 29;
            if (MagneticUi.isInside(mx, my, x, py, 22, 22)) {
                setCur(PRESETS[i]);
                return true;
            }
        }

        int btnY = gy + 214;
        if (MagneticUi.isInside(mx, my, gx, btnY, 170, 24)) {
            MagneticUi.press(gx, btnY, 170, 24);
            apply();
            return true;
        }
        if (MagneticUi.isInside(mx, my, gx + 190, btnY, 170, 24)) {
            MagneticUi.press(gx + 190, btnY, 170, 24);
            back();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragChan >= 0) {
            setChannel(dragChan, MagneticEditorUi.valueFromMouse(event.x(), gx(), W, 255));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragChan = -1;
        return super.mouseReleased(event);
    }

    // ---------------------------------------------------------------------
    // Zapis i powrót
    // ---------------------------------------------------------------------

    private void apply() {
        ModConfig.customThemeA = colA;
        ModConfig.customThemeB = colB;
        ModConfig.accentTheme = ElasticAnimation.CUSTOM_THEME;
        ModConfig.save();
        back();
    }

    private void back() {
        MagneticUi.navigate(() -> Minecraft.getInstance().setScreen(parent));
    }
}