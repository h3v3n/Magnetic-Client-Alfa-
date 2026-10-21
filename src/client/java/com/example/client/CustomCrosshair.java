package com.example.client;

import net.minecraft.client.gui.GuiGraphics;

public final class CustomCrosshair {

    public static final int SIZE = 16;
    public static final int[] pixels = new int[SIZE * SIZE]; // ARGB, alpha 0 = pusty piksel

    static {
        // Domyślny celownik: mała kropka w środku i cztery ramiona
        int w = 0xFFFFFFFF;
        put(7, 7, w); put(8, 7, w); put(7, 8, w); put(8, 8, w);
        for (int k = 3; k <= 6; k++) {
            put(7 - k, 7, w);
            put(8 + k, 7, w);
            put(7, 7 - k, w);
            put(7, 8 + k, w);
        }
    }

    private CustomCrosshair() {}

    private static void put(int x, int y, int argb) {
        pixels[y * SIZE + x] = argb;
    }

    /** Skala piksela zależna od suwaka rozmiaru celownika (1..3). */
    public static int scale() {
        return Math.max(1, Math.round(ModConfig.crosshairSize / 4.0f));
    }

    public static void draw(GuiGraphics g, int cx, int cy) {
        drawPixels(g, pixels, cx, cy, scale());
    }

    public static void drawPixels(GuiGraphics g, int[] px, int cx, int cy, int ps) {
        int half = SIZE * ps / 2;
        int x0 = cx - half;
        int y0 = cy - half;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int c = px[y * SIZE + x];
                if ((c >>> 24) == 0) continue;
                g.fill(x0 + x * ps, y0 + y * ps, x0 + (x + 1) * ps, y0 + (y + 1) * ps, c);
            }
        }
    }

    public static String encode() {
        StringBuilder sb = new StringBuilder(pixels.length * 9);
        for (int i = 0; i < pixels.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Integer.toHexString(pixels[i]));
        }
        return sb.toString();
    }

    public static void decode(String s) {
        if (s == null || s.isEmpty()) return;
        String[] parts = s.split(",");
        for (int i = 0; i < pixels.length && i < parts.length; i++) {
            try {
                pixels[i] = (int) Long.parseLong(parts[i].trim(), 16);
            } catch (NumberFormatException ignored) {
                pixels[i] = 0;
            }
        }
    }
}