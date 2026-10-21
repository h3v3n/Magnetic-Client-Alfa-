package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

public final class MagneticUi {

    private static final long PRESS_MS = 240L;
    private static final long EXIT_MS = 200L;

    private static final Map<String, Long> pressTimes = new HashMap<>();

    private static boolean exiting = false;
    private static boolean scheduled = false;
    private static Runnable exitAction = null;
    private static long exitStart = 0L;

    private MagneticUi() {}

    // ---------------------------------------------------------------------
    // Pomocnicze
    // ---------------------------------------------------------------------

    public static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static float clamp01(float v) {
        return Math.clamp(v, 0.0f, 1.0f);
    }

    public static float easeOutCubic(float t) {
        float u = 1.0f - clamp01(t);
        return 1.0f - u * u * u;
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float u = t - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }

    /** 0..1 od momentu startTime, z wygładzeniem. Przed startTime zwraca 0. */
    public static float entrance(long startTime, long durationMs) {
        return easeOutCubic((now() - startTime) / (float) durationMs);
    }

    /** Zwraca [x, y, w, h] przeskalowane wokół środka. */
    public static int[] scaled(int x, int y, int w, int h, float s) {
        int sw = Math.round(w * s);
        int sh = Math.round(h * s);
        return new int[]{x + (w - sw) / 2, y + (h - sh) / 2, sw, sh};
    }

    // ---------------------------------------------------------------------
    // Przejścia między ekranami
    // ---------------------------------------------------------------------

    /** Zaczyna wygaszanie ekranu, po czym wykonuje action (np. otwarcie następnego ekranu). */
    public static void navigate(Runnable action) {
        if (exiting) return;
        exiting = true;
        scheduled = false;
        exitAction = action;
        exitStart = now();
    }

    public static boolean isExiting() {
        return exiting;
    }

    /** Mnożnik krycia na czas wyjścia: 1 normalnie, 0 gdy ekran już zniknął. */
    public static float exitAlpha() {
        if (!exiting) return 1.0f;
        float t = clamp01((now() - exitStart) / (float) EXIT_MS);
        if (t >= 1.0f) {
            if (!scheduled) {
                scheduled = true;
                Minecraft.getInstance().execute(() -> {
                    Runnable r = exitAction;
                    exitAction = null;
                    exiting = false;
                    scheduled = false;
                    if (r != null) r.run();
                });
            }
            return 0.0f;
        }
        return 1.0f - t * t;
    }

    // ---------------------------------------------------------------------
    // Efekt wciśnięcia
    // ---------------------------------------------------------------------

    private static String key(int x, int y, int w, int h) {
        return x + "," + y + "," + w + "," + h;
    }

    /** Wywołaj w momencie kliknięcia, z tymi samymi współrzędnymi co rysowany przycisk. */
    public static void press(int x, int y, int w, int h) {
        pressTimes.put(key(x, y, w, h), now());
    }

    /** 1.0 normalnie, ~0.92 zaraz po kliknięciu, potem wraca z odbiciem. */
    public static float pressScale(int x, int y, int w, int h) {
        String k = key(x, y, w, h);
        Long t0 = pressTimes.get(k);
        if (t0 == null) return 1.0f;

        float p = clamp01((now() - t0) / (float) PRESS_MS);
        if (p >= 1.0f) {
            pressTimes.remove(k);
            return 1.0f;
        }
        if (p < 0.3f) {
            return 1.0f - 0.08f * easeOutCubic(p / 0.3f);
        }
        return 0.92f + 0.08f * easeOutBack((p - 0.3f) / 0.7f);
    }

    // ---------------------------------------------------------------------
    // Rysowanie
    // ---------------------------------------------------------------------

    /** Tło z siatką i pływającymi poświatami w kolorach motywu. */
    public static void drawBackground(GuiGraphics g, int w, int h, int mouseX, int mouseY, float fade) {
        int primary = ElasticAnimation.getPrimaryColor(0);
        int secondary = ElasticAnimation.getSecondaryColor();
        float t = (System.currentTimeMillis() % 1000000L) / 1000.0f;

        g.fill(0, 0, w, h, 0xFF05070C);

        int gridCol = ElasticAnimation.withAlpha(primary, (int) (14 * fade));
        for (int x = 0; x < w; x += 32) g.fill(x, 0, x + 1, h, gridCol);
        for (int y = 0; y < h; y += 32) g.fill(0, y, w, y + 1, gridCol);

        float px = (mouseX - w / 2.0f) * 0.02f;
        float py = (mouseY - h / 2.0f) * 0.02f;

        for (int i = 0; i < 6; i++) {
            float ang = t * (0.20f + i * 0.07f) + i * 1.3f;
            float rad = Math.min(w, h) * (0.22f + 0.05f * i);
            int cx = (int) (w / 2.0f + Math.cos(ang) * rad - px * (i + 1));
            int cy = (int) (h / 2.0f + Math.sin(ang * 0.8f) * rad * 0.6f - py * (i + 1));
            int size = 140 + i * 60;
            int col = (i % 2 == 0) ? primary : secondary;
            int alpha = (int) ((26 + 8 * Math.sin(t * 1.5f + i)) * fade);
            SmoothUiRenderer.drawSmoothCircle(g, cx - size / 2, cy - size / 2, size,
                    ElasticAnimation.withAlpha(col, Math.max(0, alpha)));
        }
    }

    /** Ciemny szklany panel. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, float fade) {
        int primary = ElasticAnimation.getPrimaryColor(0);
        SmoothUiRenderer.drawSmoothCard(g, x, y, w, h, 16,
                ElasticAnimation.withAlpha(0x080C14, (int) (240 * fade)),
                ElasticAnimation.withAlpha(primary, (int) (195 * fade)),
                ElasticAnimation.withAlpha(primary, (int) (95 * fade)));
    }

    /** Panel, który powiększa się przy wejściu (od 94% do 100% z lekkim odbiciem). */
    public static void drawPopPanel(GuiGraphics g, int x, int y, int w, int h, float fade, long openTime) {
        float t = clamp01((now() - openTime) / 380.0f);
        float s = 0.94f + 0.06f * easeOutBack(t);
        int[] r = scaled(x, y, w, h, s);
        drawPanel(g, r[0], r[1], r[2], r[3], fade);
    }

    /** Przycisk w stylu menu: reaguje na najechanie i wciśnięcie. */
    public static void drawButton(GuiGraphics g, Font font, int x, int y, int w, int h, String label,
                                  boolean hovered, boolean active, float fade) {
        int primary = ElasticAnimation.getPrimaryColor(0);
        boolean lit = hovered || active;

        float s = pressScale(x, y, w, h) * (hovered ? 1.02f : 1.0f);
        int[] r = scaled(x, y, w, h, s);
        int sx = r[0], sy = r[1], sw = r[2], sh = r[3];

        int bg = ElasticAnimation.withAlpha(hovered ? 0xFF1A2438 : 0xFF0E131F, (int) (221 * fade));
        int border = ElasticAnimation.withAlpha(lit ? primary : 0xFF2A3446, (int) (255 * fade));
        int glow = ElasticAnimation.withAlpha(primary, (int) ((lit ? 60 : 0) * fade));
        SmoothUiRenderer.drawSmoothCard(g, sx, sy, sw, sh, 9, bg, border, glow);

        g.fill(sx + 6, sy + 6, sx + 8, sy + sh - 6,
                ElasticAnimation.withAlpha(primary, (int) ((lit ? 255 : 80) * fade)));

        int textCol = ElasticAnimation.withAlpha(hovered ? 0xFFFFFFFF : 0xFFE2E8F0, (int) (255 * fade));
        g.drawString(font, label, sx + (sw - font.width(label)) / 2, sy + (sh - 8) / 2, textCol, true);
    }

    /** Nagłówek: tytuł z obwódką, akcentowa linia i podtytuł. cx = środek w poziomie. */
    public static void drawHeader(GuiGraphics g, Font font, String title, String sub, int cx, int y, float fade) {
        int primary = ElasticAnimation.getPrimaryColor(0);
        int secondary = ElasticAnimation.getSecondaryColor();

        int tx = cx - font.width(title) / 2;
        int tint = ElasticAnimation.withAlpha(primary, (int) (255 * fade));
        int white = ElasticAnimation.withAlpha(0xFFFFFF, (int) (255 * fade));

        g.drawString(font, title, tx - 1, y, tint, false);
        g.drawString(font, title, tx + 1, y, tint, false);
        g.drawString(font, title, tx, y - 1, tint, false);
        g.drawString(font, title, tx, y + 1, tint, false);
        g.drawString(font, title, tx, y, white, false);

        g.fill(cx - 70, y + 16, cx + 70, y + 17, ElasticAnimation.withAlpha(primary, (int) (200 * fade)));

        g.drawString(font, sub, cx - font.width(sub) / 2, y + 22,
                ElasticAnimation.withAlpha(secondary, (int) (255 * fade)), true);
    }
}