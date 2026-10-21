package com.example.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class MagneticEditorUi {

    private MagneticEditorUi() {}

    /** Suwak z etykietą i wartością. */
    public static void slider(GuiGraphics g, Font f, int x, int y, int w, int h,
                              String label, int value, int max, int accent, boolean hovered) {
        int border = hovered ? ElasticAnimation.withAlpha(accent, 180) : 0x552A354B;
        SmoothUiRenderer.drawSmoothCard(g, x, y, w, h, 7, 0xD90D121D, border, 0);

        int trackX = x + 7;
        int trackW = w - 14;
        int fill = Math.round(trackW * (value / (float) max));
        SmoothUiRenderer.drawSmoothRoundedRect(g, trackX, y + h - 6, trackW, 4, 2, 0xFF1A2234);
        SmoothUiRenderer.drawSmoothRoundedRect(g, trackX, y + h - 6, Math.max(4, fill), 4, 2, accent);

        g.drawString(f, label, x + 7, y + 3, 0xFFE2E8F0, true);
        String v = String.valueOf(value);
        g.drawString(f, v, x + w - f.width(v) - 7, y + 3, accent, true);
    }

    /** Wartość suwaka (0..max) z pozycji X myszy. */
    public static int valueFromMouse(double mx, int x, int w, int max) {
        float pct = Math.clamp((float) ((mx - (x + 7)) / (w - 14)), 0.0f, 1.0f);
        return Math.round(pct * max);
    }

    /** Kwadratowy kolorowy przycisk z obramowaniem. */
    public static void swatch(GuiGraphics g, int x, int y, int size, int color, boolean highlighted, int accent) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, highlighted ? accent : 0xFF2A3446);
        int fillCol = ((color >>> 24) == 0) ? 0xFF141B2A : color;
        g.fill(x, y, x + size, y + size, fillCol);
    }
}